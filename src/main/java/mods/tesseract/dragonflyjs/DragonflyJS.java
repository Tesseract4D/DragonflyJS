package mods.tesseract.dragonflyjs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnType;
import net.tclproject.mysteriumlib.asm.annotations.FixOrder;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.FixInserterFactory;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.Type;

import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.util.ArrayList;
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
    public static ArrayList<IFunction> preInitEvents = new ArrayList<>();
    public static ArrayList<IFunction> initEvents = new ArrayList<>();
    public static ArrayList<IFunction> postInitEvents = new ArrayList<>();

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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        for (IFunction f : preInitEvents)
            f.invoke(e);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        for (IFunction f : initEvents)
            f.invoke(e);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        for (IFunction f : postInitEvents)
            f.invoke(e);
    }

    public static void registerPreInitEvent(IFunction f) {
        preInitEvents.add(f);
    }

    public static void registerInitEvent(IFunction f) {
        initEvents.add(f);
    }

    public static void registerPostInitEvent(IFunction f) {
        postInitEvents.add(f);
    }

    public static void registerJSFix(ScriptObjectMirror fn, ScriptObjectMirror obj) {
        if (!fn.isFunction())
            throw new IllegalArgumentException("参数" + fn + " 不是一个函数！");
        if (!obj.getClassName().equals("Object"))
            throw new IllegalArgumentException("参数" + obj + " 不是一个对象！");
        JSProxy.cachedFunctions.add(fn);

        ASMFix.Builder builder = ASMFix.newBuilder();
        String[] keys = obj.getOwnKeys(true);
        int onLine = -2;
        boolean returnedValue = false;
        String targetDesc = "", targetMethod = "", onInvoke = "";
        EnumReturnSetting setting = null;
        Object constant = null;

        for (String key : keys) {
            Object o = obj.get(key);
            switch (key) {
                case "targetDesc" -> {
                    String s = (String) o;
                    int i = s.indexOf('(');
                    targetDesc = s.substring(i);
                    targetMethod = s.substring(0, i);
                }
                case "returnSetting" -> setting = EnumReturnSetting.valueOf((String) o);
                case "order" -> builder.setPriority(FixOrder.valueOf((String) o));
                case "createNewMethod" -> builder.setCreateMethod(Boolean.TRUE.equals(o));
                case "isFatal" -> builder.setFatal(Boolean.TRUE.equals(o));
                case "returnedValue" -> returnedValue = Boolean.TRUE.equals(o);
                case "insertOnLine" -> onLine = (int) o;
                case "insertOnInvoke" -> onInvoke = (String) o;
                case "insertOnExit" -> {
                    if (Boolean.TRUE.equals(o))
                        builder.setInjectorFactory(ASMFix.ON_EXIT_FACTORY);
                }
                case "nullReturned" -> builder.setReturnType(EnumReturnType.NULL);
                case "constantAlwaysReturned" -> constant = o;
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

        if (constant != null) {
            builder.setReturnType(EnumReturnType.PRIMITIVE_CONSTANT);
            builder.setPrimitiveAlwaysReturned(constant);
        }

        builder.addThisToFixMethodParameters();
        int currentParameterId = 1;
        for (int i = 1; i < types.length; i++) {
            Type type = types[i];
            if (returnedValue && i == types.length - 1) {
                builder.setTargetMethodReturnType(type);
                builder.addReturnedValueToFixMethodParameters();
            } else {
                builder.addTargetMethodParameters(type);
                builder.addFixMethodParameter(type, currentParameterId);
                currentParameterId += type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
            }
        }

        registerFix(builder.build());
    }

    @Override
    public void registerFixes() {
        File scriptDir = new File(Launch.minecraftHome, "djs");
        scriptDir.mkdir();
        registerClassWithFixes("mods.tesseract.dragonflyjs.fix.FixesDragonfly");
        try {
            DragonflyJS.instance.nashorn.eval("""
                var global=this;
                function importClass(s){
                 global[s.substring(s.lastIndexOf(".")+1)]=Java.type(s);
                }
                function registerEvent(c,b,e){
                 (new c).getListenerList().register(b,Java.type("cpw.mods.fml.common.eventhandler.EventPriority").NORMAL,e);
                }
                function getSide(){
                 return Java.type("cpw.mods.fml.common.FMLCommonHandler").instance().getSide();
                }
                importClass("mods.tesseract.dragonflyjs.DragonflyJS");
                """);
            File[] files = scriptDir.listFiles();
            StringBuilder sb = new StringBuilder();
            if (files != null)
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".js"))
                        sb.append(FileUtils.readFileToString(file));
                    if (sb.length() != 0)
                        sb.append("\n");
                }
            if (sb.length() != 0) {
                DragonflyJS.instance.nashorn.eval(sb.toString());
            }
        } catch (ScriptException | IOException e) {
            throw new RuntimeException(e);
        }

        try {
            byte[] data = WrapperClassVisitor.visit();
            DragonflyJS.defineClass.invoke(Launch.classLoader, wrapperClass, data, 0, data.length, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
