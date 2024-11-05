package mods.tesseract.dragonflyjs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import jdk.nashorn.api.scripting.NashornScriptEngine;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
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
        //System.out.println("&" + MiscUtils.getMemberInfo(MapGenCaves.class.getDeclaredMethod("isExceptionBiome", BiomeGenBase.class)));
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }

    @Override
    public void registerFixes() {
        Object r;
        try {
            r = DragonflyJS.instance.nashorn.eval("""
                var global=this;
                function imp(s){
                 global[s.substring(s.lastIndexOf(".")+1)]=Java.type(s);
                }

                imp("java.lang.Thread");
                imp("java.lang.Class");
                imp("mods.tesseract.dragonflyjs.DragonflyJS");

                function jsmethod(){
                 print("&");
                 for each(var i in arguments)
                  print(i);
                }
                function a(a){
                 var s=Thread.currentThread().getStackTrace();
                 return Class.forName(s[1].getClassName());
                }
                DragonflyJS.instance.registerJSFix(jsmethod,{"aa":"bb"});
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

    public void registerJSFix(ScriptObjectMirror fn, ScriptObjectMirror obj) {
        if (!fn.isFunction())
            throw new IllegalArgumentException();
        JSProxy.cachedFunctions.add(fn);
        registerFix(createJSFix());
    }

    public static ASMFix createJSFix() {
        String targetDesc = "(Lnet/minecraft/world/gen/MapGenCaves;Lnet/minecraft/world/biome/BiomeGenBase;)V", targetMethod = "isExceptionBiome";
        String fixMethod = "js$" + targetMethod + "$" + fixIndex++;
        Type[] types = Type.getArgumentTypes(targetDesc);
        if (types.length == 0)
            throw new IllegalArgumentException();
        WrapperClassVisitor.createMethod(fixMethod, targetDesc);
        ASMFix.Builder builder = ASMFix.newBuilder();

        builder.setTargetClass(types[0].getClassName());
        builder.setTargetMethod(targetMethod);
        builder.setFixesClass("mods.tesseract.dragonflyjs.JSWrapper");
        builder.setFixMethod(fixMethod);

        builder.addThisToFixMethodParameters();
        int currentParameterId = 1;
        for (int i = 1; i < types.length; i++) {
            Type type = types[i];
            builder.addTargetMethodParameters(type);
            builder.addFixMethodParameter(type, currentParameterId);
            currentParameterId += type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
        }

        return builder.build();
    }
}
