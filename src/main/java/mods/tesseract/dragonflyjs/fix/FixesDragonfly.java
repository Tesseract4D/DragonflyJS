package mods.tesseract.dragonflyjs.fix;

import mods.tesseract.dragonflyjs.DragonflyJS;
import net.minecraft.util.MathHelper;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.annotations.ReturnedValue;

import java.security.CodeSource;

public class FixesDragonfly{
    @Fix(targetClass = "jdk.nashorn.internal.runtime.ScriptLoader", returnSetting = EnumReturnSetting.ALWAYS, insertOnExit = true)
    public static Class<?> installClass(Object c, String name, byte[] data, CodeSource cs, @ReturnedValue Class<?> z) {
        DragonflyJS.cachedClasses.put(name, z);
        return z;
    }
}
