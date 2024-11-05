package mods.tesseract.dragonflyjs;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class WrapperClassVisitor {
    private static final ArrayList<String> methods = new ArrayList<>();

    public static byte[] visit() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, DragonflyJS.wrapperClass.replace('.', '/'), null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        for (int i = 0; i < methods.size(); i += 2)
            insertMethod(cw, methods.get(i), methods.get(i + 1), i / 2);

        cw.visitEnd();
        try {
            FileUtils.writeByteArrayToFile(new File(Launch.minecraftHome, "JSWrapper.class"), cw.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return cw.toByteArray();
    }

    public static void createMethod(String name, String desc) {
        methods.add(name);
        methods.add(desc);
    }

    public static void insertMethod(ClassVisitor cw, String name, String desc, int index) {
        StringBuilder desc2 = new StringBuilder("(I");
        Type[] types = Type.getArgumentTypes(desc);
        Type rt = Type.getReturnType(desc);
        int n = types.length, m = n - 1;
        int[] stores = new int[n];
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, name, desc, null, null);
        mv.visitCode();
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (type.getSort() <= 8) {
                stores[i] = ++m;
                insertLoad(mv, type, i);
                insertPrimitiveToObject(mv, type);
                mv.visitVarInsn(ASTORE, m);
            } else {
                stores[i] = i;
            }
            desc2.append("Ljava/lang/Object;");
        }
        desc2.append(")Ljava/lang/Object;");

        mv.visitLdcInsn(index);
        for (int j : stores) {
            mv.visitVarInsn(ALOAD, j);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "mods/tesseract/dragonflyjs/JSProxy", "invokeJS", desc2.toString(), false);
        if (rt.getSort() != VOID) {
            insertObjectToPrimitive(mv, rt);
        }
        insertReturn(mv, rt);
        mv.visitMaxs(3, m + 1);
        mv.visitEnd();
    }

    public static void insertPrimitiveToObject(MethodVisitor mv, Type primitive) {
        switch (primitive.getSort()) {
            case Type.INT ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            case Type.LONG ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            case Type.DOUBLE ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            case Type.FLOAT ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            case Type.BOOLEAN ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            case Type.CHAR ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            case Type.BYTE ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            case Type.SHORT ->
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            default -> throw new UnsupportedOperationException("Unsupported type: " + primitive);
        }
    }

    public static void insertObjectToPrimitive(MethodVisitor mv, Type primitive) {
        switch (primitive.getSort()) {
            case INT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
            }
            case BOOLEAN -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            }
            case Type.FLOAT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
            }
            case Type.DOUBLE -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
            }
            case BYTE -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
            }
            case SHORT -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
            }
            case CHAR -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
            }
            case Type.LONG -> {
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
            }
            default -> mv.visitTypeInsn(Opcodes.CHECKCAST, primitive.getInternalName());
        }
    }

    public static void insertReturn(MethodVisitor inserter, Type targetMethodReturnType) {
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

    public static void insertLoad(MethodVisitor inserter, Type parameterType, int variableIndex) {
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
}
