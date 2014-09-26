package org.jetbrains.interpret

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import java.util.HashMap
import java.lang.reflect.Constructor

class BackendEmitter(val emitKlass: String, val protoName: String) : org.objectweb.asm.ClassVisitor(org.objectweb.asm.Opcodes.ASM4) {
    val writer = org.objectweb.asm.ClassWriter(0)
    val klassBuilder = writer// TraceClassVisitor(writer, ASMifier(), PrintWriter(System.out))
    val properties = java.util.HashMap<String, String>()
    val backendEntityKlass = javaClass<Storage>().getName().replace(".", "/")
    val backendPropertyKlass = javaClass<EntityBackendProperty<*>>().getName().replace(".", "/")
    val backendKlass = javaClass<EntityBackend>().getName().replace(".", "/")
    val backendPropertyType = org.objectweb.asm.Type.getObjectType(backendPropertyKlass)!!.getDescriptor()!!

    private fun getBoxedType(klass: org.objectweb.asm.Type): org.objectweb.asm.Type =
            when (klass.getSort()) {
                org.objectweb.asm.Type.BYTE -> org.objectweb.asm.Type.getObjectType("java/lang/Byte")!!
                org.objectweb.asm.Type.BOOLEAN -> org.objectweb.asm.Type.getObjectType("java/lang/Boolean")!!
                org.objectweb.asm.Type.SHORT -> org.objectweb.asm.Type.getObjectType("java/lang/Short")!!
                org.objectweb.asm.Type.CHAR -> org.objectweb.asm.Type.getObjectType("java/lang/Character")!!
                org.objectweb.asm.Type.INT -> org.objectweb.asm.Type.getObjectType("java/lang/Integer")!!
                org.objectweb.asm.Type.FLOAT -> org.objectweb.asm.Type.getObjectType("java/lang/Float")!!
                org.objectweb.asm.Type.LONG -> org.objectweb.asm.Type.getObjectType("java/lang/Long")!!
                org.objectweb.asm.Type.DOUBLE -> org.objectweb.asm.Type.getObjectType("java/lang/Double")!!
                else -> klass
            }

    fun setProperty(propertyName: String, propertyType: String) {
        val asmType = org.objectweb.asm.Type.getType(propertyType)
        val lowerPropertyName = propertyName.decapitalize()

        properties.put(lowerPropertyName, propertyType)

        // add getter
        val setter = klassBuilder.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "set$propertyName", "($propertyType)V", null, null)!!
        setter.visitCode()
        setter.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
        setter.visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, emitKlass, "$lowerPropertyName\$delegate", "L$backendPropertyKlass;")
        setter.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
        setter.visitLdcInsn("$lowerPropertyName")
        when (propertyType) {
            "I" -> {
                setter.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 1)
                setter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
            }
            "Z" -> {
                setter.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 1)
                setter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;")
            }
            "J" -> {
                setter.visitVarInsn(org.objectweb.asm.Opcodes.LLOAD, 1)
                setter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;")
            }
            "D" -> {
                setter.visitVarInsn(org.objectweb.asm.Opcodes.DLOAD, 1)
                setter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;")
            }
            else -> {
                setter.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1)
            }
        }

        setter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, backendPropertyKlass, "set", "(L$backendKlass;Ljava/lang/String;Ljava/lang/Object;)V")
        setter.visitInsn(org.objectweb.asm.Opcodes.RETURN)
        setter.visitMaxs(5, 3)
        setter.visitEnd()

    }

    fun getProperty(propertyName: String, propertyType: String) {
        val asmType = org.objectweb.asm.Type.getType(propertyType)
        val lowerPropertyName = propertyName.decapitalize()

        properties.put(lowerPropertyName, propertyType)

        // add getter
        val getter = klassBuilder.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "get$propertyName", "()$propertyType", null, null)!!
        getter.visitCode()
        getter.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
        getter.visitFieldInsn(org.objectweb.asm.Opcodes.GETFIELD, emitKlass, "$lowerPropertyName\$delegate", "L$backendPropertyKlass;")
        getter.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
        getter.visitLdcInsn("$lowerPropertyName")
        getter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, backendPropertyKlass, "get", "(L$backendKlass;Ljava/lang/String;)Ljava/lang/Object;")
        when (propertyType) {
            "I" -> {
                getter.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I")
                getter.visitInsn(org.objectweb.asm.Opcodes.IRETURN)
            }
            "Z" -> {
                getter.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "java/lang/Boolean")
                getter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z")
                getter.visitInsn(org.objectweb.asm.Opcodes.IRETURN)
            }
            "J" -> {
                getter.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J")
                getter.visitInsn(org.objectweb.asm.Opcodes.LRETURN)
            }
            "D" -> {
                getter.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D")
                getter.visitInsn(org.objectweb.asm.Opcodes.DRETURN)
            }
            else -> {
                getter.visitTypeInsn(org.objectweb.asm.Opcodes.CHECKCAST, "${asmType!!.getInternalName()}")
                getter.visitInsn(org.objectweb.asm.Opcodes.ARETURN)
            }
        }
        getter.visitMaxs(5, 1)
        getter.visitEnd()
    }

    override fun visitEnd() {
        // add fields
        for ((lowerPropertyName, propertyType) in properties) {
            val backingField = klassBuilder.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE + org.objectweb.asm.Opcodes.ACC_FINAL, "$lowerPropertyName\$delegate", backendPropertyType,
                                                       "L$backendPropertyKlass<$propertyType>;", null)!!
            backingField.visitEnd()
        }

        // add constructor
        val ctor = klassBuilder.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC, "<init>", "(L$backendEntityKlass;)V", null, null)!!
        ctor.visitCode()
        ctor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1)
        ctor.visitLdcInsn("entity")
        ctor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V")
        ctor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
        ctor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 1)
        ctor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, backendKlass, "<init>", "(L$backendEntityKlass;)V")

        for ((lowerPropertyName, propertyType) in properties) {
            ctor.visitVarInsn(org.objectweb.asm.Opcodes.ALOAD, 0)
            ctor.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, backendPropertyKlass)
            ctor.visitInsn(org.objectweb.asm.Opcodes.DUP)
            ctor.visitLdcInsn(getBoxedType(org.objectweb.asm.Type.getType(propertyType)!!))
            ctor.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESPECIAL, backendPropertyKlass, "<init>", "(Ljava/lang/Class;)V")
            ctor.visitFieldInsn(org.objectweb.asm.Opcodes.PUTFIELD, emitKlass, "$lowerPropertyName\$delegate", backendPropertyType)
        }
        ctor.visitInsn(org.objectweb.asm.Opcodes.RETURN)
        ctor.visitMaxs(5, 3)
        ctor.visitEnd()

        klassBuilder.visitEnd()
        org.objectweb.asm.ClassVisitor<org.objectweb.asm.ClassVisitor>.visitEnd()
    }

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        klassBuilder.visit(org.objectweb.asm.Opcodes.V1_7, org.objectweb.asm.Opcodes.ACC_FINAL + org.objectweb.asm.Opcodes.ACC_SUPER, emitKlass, null, javaClass<EntityBackend>().getName().replace(".", "/"), array(protoName))
        org.objectweb.asm.ClassVisitor<org.objectweb.asm.ClassVisitor>.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): org.objectweb.asm.MethodVisitor? {
        name!!
        desc!!

        if (name.startsWith("get"))
            getProperty(name.drop(3), desc.dropWhile { it == '(' || it == ')' })
        else if (name.startsWith("set"))
            setProperty(name.drop(3), desc.dropWhile { it == '(' }.takeWhile { it != ')' })
        return org.objectweb.asm.ClassVisitor<org.objectweb.asm.ClassVisitor>.visitMethod(access, name, desc, signature, exceptions)
    }
    fun getBytes(): ByteArray = writer.toByteArray()!!
}

/*
fun dump(prototype: Class<*>) {
    val cr = ClassReader(prototype.getName())
    val cv = TraceClassVisitor(null, ASMifier(), PrintWriter(System.out))
    cr.accept(cv, 0)
}
*/
