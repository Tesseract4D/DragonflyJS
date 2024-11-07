package mods.tesseract.dragonflyjs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minecraft.block.Block;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.world.gen.MapGenCaves;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.FixOrder;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.FixInserterFactory;
import net.tclproject.mysteriumlib.asm.core.MiscUtils;
import org.objectweb.asm.Type;

import javax.script.Invocable;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.Map;

@Mod(modid = "djs")
public class DragonflyJS extends CustomLoadingPlugin {
    public static DragonflyJS instance;
    public final NashornScriptEngine nashorn;
    public static Map<String, Class<?>> cachedClasses;
    public static Method defineClass;
    public static Field sobj;
    public static final String wrapperClass = "mods.tesseract.dragonflyjs.JSWrapper";
    public static int fixIndex = 0;

    static {
        try {
            Launch.classLoader.addURL(LaunchClassLoader.getSystemClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngine").getProtectionDomain().getCodeSource().getLocation());
            Field f = LaunchClassLoader.class.getDeclaredField("cachedClasses");
            f.setAccessible(true);
            defineClass = SecureClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
            defineClass.setAccessible(true);
            sobj = ScriptObjectMirror.class.getDeclaredField("sobj");
            sobj.setAccessible(true);
            cachedClasses = (Map<String, Class<?>>) f.get(Launch.classLoader);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public DragonflyJS() {
        instance = this;
        nashorn = (NashornScriptEngine) new ScriptEngineManager().getEngineByName("nashorn");
    }

    public static Object invokeJS(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return ((Invocable) DragonflyJS.instance.nashorn).invokeFunction(name, args);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) throws NoSuchMethodException {
        System.out.println("&" + MiscUtils.getMemberInfo(MapGenCaves.class.getDeclaredMethod("digBlock", Block[].class, int.class, int.class, int.class, int.class, int.class, int.class, boolean.class)));
    }

    @Override
    public void registerFixes() {
        try {
            DragonflyJS.instance.nashorn.eval("""
                var global=this;
                function imp(s){
                 global[s.substring(s.lastIndexOf(".")+1)]=Java.type(s);
                }

                imp("java.lang.Thread");
                imp("java.lang.Class");
                imp("mods.tesseract.dragonflyjs.DragonflyJS");
                imp("org.lwjgl.opengl.Display");

                function j(){
                 print("aa");
                }
                function a(a){
                 print("&&");
                 Display.setTitle("Custom Title");
                 return true;
                }
                DragonflyJS.registerJSFix(j,{
                 "targetDesc":"(Lnet/minecraft/world/gen/MapGenCaves;[Lnet/minecraft/block/Block;IIIIIIZ)V",
                 "targetMethod":"digBlock"
                });
                DragonflyJS.registerJSFix(a,{
                 "targetDesc":"(Lnet/minecraft/client/Minecraft;JJDDZCIS)Z",
                 "targetMethod":"startGame",
                 "returnSetting":"ON_TRUE"
                });
                """);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }

        try {
            byte[] data = WrapperClassVisitor.visit();
            DragonflyJS.defineClass.invoke(Launch.classLoader, wrapperClass, data, 0, data.length, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        registerClassWithFixes("mods.tesseract.dragonflyjs.fix.Fixes");
    }

    public static void registerJSFix(ScriptObjectMirror fn, ScriptObjectMirror obj) {
        if (!fn.isFunction())
            throw new IllegalArgumentException("参数" + fn + " 不是一个函数！");
        if (!obj.getClassName().equals("Object"))
            throw new IllegalArgumentException("参数" + obj + " 不是一个对象！");
        JSProxy.cachedFunctions.add(fn);

        ASMFix.Builder builder = ASMFix.newBuilder();
        String[] keys = obj.getOwnKeys(true);
        String targetDesc = "", targetMethod = "";
        EnumReturnSetting setting = null;
        String onInvoke = "";
        int onLine = -2;

        for (String key : keys) {
            switch (key) {
                case "targetDesc" -> targetDesc = (String) obj.get(key);
                case "targetMethod" -> targetMethod = (String) obj.get(key);
                case "returnSetting" -> setting = EnumReturnSetting.valueOf((String) obj.get(key));
                case "order" -> builder.setPriority(FixOrder.valueOf((String) obj.get(key)));
                case "createNewMethod" -> builder.setCreateMethod(Boolean.TRUE.equals(obj.get(key)));
                case "isFatal" -> builder.setFatal(Boolean.TRUE.equals(obj.get(key)));
                case "insertOnLine" -> onLine = (int) obj.get(key);
                case "insertOnInvoke" -> onInvoke = (String) obj.get(key);
                case "insertOnExit" -> {
                    if (Boolean.TRUE.equals(obj.get(key)))
                        builder.setInjectorFactory(ASMFix.ON_EXIT_FACTORY);
                }
            }
        }

        if (targetDesc.isEmpty())
            throw new IllegalArgumentException("目标方法描述不能为空！");
        if (targetMethod.isEmpty())
            throw new IllegalArgumentException("目标方法名称不能为空！");
        if (!onInvoke.isEmpty()) {
            builder.setInjectorFactory(new FixInserterFactory.OnInvoke(onInvoke, onLine));
        } else if (onLine >= 0) {
            builder.setInjectorFactory(new FixInserterFactory.OnLineNumber(onLine));
        }

        String fixMethod = "js$" + targetMethod + "$" + fixIndex++;
        Type[] types = Type.getArgumentTypes(targetDesc);
        if (types.length == 0)
            throw new IllegalArgumentException();
        WrapperClassVisitor.createMethod(fixMethod, targetDesc);

        builder.setTargetClass(types[0].getClassName());
        builder.setTargetMethod(targetMethod);
        builder.setFixesClass("mods.tesseract.dragonflyjs.JSWrapper");
        builder.setFixMethod(fixMethod);
        if (setting != null)
            builder.setReturnSetting(setting);

        builder.addThisToFixMethodParameters();
        int currentParameterId = 1;
        for (int i = 1; i < types.length; i++) {
            Type type = types[i];
            builder.addTargetMethodParameters(type);
            builder.addFixMethodParameter(type, currentParameterId);
            currentParameterId += type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
        }

        registerFix(builder.build());
    }
}
