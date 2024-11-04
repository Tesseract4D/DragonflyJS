package mods.tesseract.dragonflyjs;

import net.tclproject.mysteriumlib.asm.core.ASMFix;
import net.tclproject.mysteriumlib.asm.core.FixInserter;
import net.tclproject.mysteriumlib.asm.core.FixInserterClassVisitor;
import org.objectweb.asm.MethodVisitor;

public class JSFixInserter extends FixInserter {
    public JSFixInserter(MethodVisitor mv, int access, String name, String descriptor, ASMFix fix, FixInserterClassVisitor classVisitor) {
        super(mv, access, name, descriptor, fix, classVisitor);
    }
}
