package mods.tesseract.dragonflyjs;

import org.objectweb.asm.*;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class JSClassVisitor extends ClassVisitor {
    public JSClassVisitor(ClassWriter cw) {
        super(Opcodes.ASM5, cw);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return super.visitMethod(access, name, desc, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        String desc = "(Lmods/tesseract/dragonflyjs/Test;I)V";
        Type[] t = Type.getArgumentTypes(desc);
        int n = t.length, m = n;
        int[] r = new int[n];
        MethodVisitor mv = this.visitMethod(ACC_PUBLIC | ACC_STATIC, "b", desc, null, null);
        mv.visitCode();
        mv.visitLabel(new Label());
        for (int i = 0; i < t.length; i++) {
            Type p = t[i];
            if (p.getSort() <= 8) {
                insertLoad(mv, p, i);
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "your/package/YourClass", "yourStaticMethod", "(I)V", false);
                r[i] = ++m;
                mv.visitVarInsn(ALOAD, m);
            } else {
                r[i] = i;
            }
        }
        for (int i = 0; i < t.length; i++) {
            insertLoad(mv, t[i], r[i]);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "your/package/YourClass", "yourStaticMethod", "(Lmods/tesseract/dragonflyjs/Test;Ljava/lang/Object;)V", false);
        insertPushDefaultReturnValue(mv, VOID_TYPE);
        insertReturn(mv, VOID_TYPE);
        mv.visitLabel(new Label());
        mv.visitMaxs(3, m);
        mv.visitEnd();
        super.visitEnd();
    }

    public void insertReturn(MethodVisitor inserter, Type targetMethodReturnType) {
        if (targetMethodReturnType == INT_TYPE || targetMethodReturnType == SHORT_TYPE ||
            targetMethodReturnType == BOOLEAN_TYPE || targetMethodReturnType == BYTE_TYPE
            || targetMethodReturnType == CHAR_TYPE) {
            inserter.visitInsn(IRETURN);
        } else if (targetMethodReturnType == LONG_TYPE) {
            inserter.visitInsn(LRETURN);
        } else if (targetMethodReturnType == FLOAT_TYPE) {
            inserter.visitInsn(FRETURN);
        } else if (targetMethodReturnType == DOUBLE_TYPE) {
            inserter.visitInsn(DRETURN);
        } else if (targetMethodReturnType == VOID_TYPE) {
            inserter.visitInsn(RETURN);
        } else {
            inserter.visitInsn(ARETURN);
        }
    }

    public void insertLoad(MethodVisitor inserter, Type parameterType, int variableIndex) {
        int opcode;
        if (parameterType == INT_TYPE || parameterType == BYTE_TYPE || parameterType == CHAR_TYPE ||
            parameterType == BOOLEAN_TYPE || parameterType == SHORT_TYPE) {
            opcode = ILOAD;
        } else if (parameterType == LONG_TYPE) {
            opcode = LLOAD;
        } else if (parameterType == FLOAT_TYPE) {
            opcode = FLOAD;
        } else if (parameterType == DOUBLE_TYPE) {
            opcode = DLOAD;
        } else {
            opcode = ALOAD;
        }
        inserter.visitVarInsn(opcode, variableIndex);
    }

    public void insertPushDefaultReturnValue(MethodVisitor inserter, Type targetMethodReturnType) {
        switch (targetMethodReturnType.getSort()) {
            case Type.VOID:
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                inserter.visitInsn(Opcodes.ICONST_0);
                break;
            case Type.FLOAT:
                inserter.visitInsn(Opcodes.FCONST_0);
                break;
            case Type.LONG:
                inserter.visitInsn(Opcodes.LCONST_0);
                break;
            case Type.DOUBLE:
                inserter.visitInsn(Opcodes.DCONST_0);
                break;
            default:
                inserter.visitInsn(Opcodes.ACONST_NULL);
                break;
        }
    }
}
