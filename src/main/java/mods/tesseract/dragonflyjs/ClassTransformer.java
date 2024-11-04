package mods.tesseract.dragonflyjs;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.File;
import java.io.IOException;

public class ClassTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!transformedName.equals("mods.tesseract.dragonflyjs.Test"))
            return basicClass;
        ClassReader cr = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(0);
        cr.accept(new JSClassVisitor(cw), 0);
        byte[] b=cw.toByteArray();
        try {
            FileUtils.writeByteArrayToFile(new File(Launch.minecraftHome, "transform.class"), b);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return b;
    }
}
