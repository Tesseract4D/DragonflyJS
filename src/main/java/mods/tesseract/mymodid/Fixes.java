package mods.tesseract.mymodid;

import net.minecraft.client.Minecraft;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import org.lwjgl.opengl.Display;

public class Fixes {
    @Fix(insertOnInvoke = "org/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V")
    public static void startGame(Minecraft c){
        Display.setTitle("Custom Title");
    }
}
