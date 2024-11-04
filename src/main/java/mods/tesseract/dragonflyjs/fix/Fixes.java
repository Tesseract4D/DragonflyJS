package mods.tesseract.dragonflyjs.fix;

import mods.tesseract.dragonflyjs.DragonflyJS;
import mods.tesseract.dragonflyjs.Test;
import net.minecraft.client.Minecraft;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.annotations.ReturnedValue;
import org.lwjgl.opengl.Display;

import java.security.CodeSource;

public class Fixes {
    @Fix(targetClass = "jdk.nashorn.internal.runtime.ScriptLoader", returnSetting = EnumReturnSetting.ALWAYS, insertOnExit = true)
    public static Class<?> installClass(Object c, String name, byte[] data, CodeSource cs, @ReturnedValue Class<?> z) {
        DragonflyJS.cachedClasses.put(name, z);
        return z;
    }

    @Fix(targetClass = "net.minecraft.client.Minecraft", insertOnInvoke = "org/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V")
    public static void startGame(Object c) {
        Display.setTitle("Custom Title");
    }
}
