package mods.tesseract.dragonflyjs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;
import net.tclproject.mysteriumlib.asm.core.ASMFix;
import org.objectweb.asm.Type;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.lang.reflect.Field;
import java.util.Map;

@Mod(modid = "djs", acceptedMinecraftVersions = "[1.7.10]")
public class DragonflyJS extends CustomLoadingPlugin {
    public static DragonflyJS instance;
    public final ScriptEngine nashorn;
    public static Map<String, Class<?>> cachedClasses;

    static {
        try {
            Launch.classLoader.addURL(LaunchClassLoader.getSystemClassLoader().loadClass("jdk.nashorn.api.scripting.NashornScriptEngine").getProtectionDomain().getCodeSource().getLocation());
            Field f = LaunchClassLoader.class.getDeclaredField("cachedClasses");
            f.setAccessible(true);
            cachedClasses = (Map<String, Class<?>>) f.get(Launch.classLoader);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public DragonflyJS() {
        instance = this;
        nashorn = new ScriptEngineManager().getEngineByName("nashorn");
    }

    public static Object invokeJS(String name, Object... args) throws ScriptException, NoSuchMethodException {
        return ((Invocable) DragonflyJS.instance.nashorn).invokeFunction(name, args);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Test.a();
    }

    public static void b() {
        System.out.println("@");
    }
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ClassTransformer.class.getName(), FirstClassTransformer.class.getName()};
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
                function a(a){
                 var s=Thread.currentThread().getStackTrace();
                 return Class.forName(s[1].getClassName());
                }
                function b(){
                 return {"a":1};
                }
                a()
                """);
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        ASMFix.Builder builder = ASMFix.newBuilder();
        builder.setTargetClass("mods.tesseract.dragonflyjs.Test");
        builder.setTargetMethod("a");
        builder.setFixesClass("mods.tesseract.dragonflyjs.Test");
        builder.setFixMethod("b");
        builder.addThisToFixMethodParameters();
        registerFix(builder.build());
        registerClassWithFixes("mods.tesseract.dragonflyjs.fix.Fixes");
    }
}
