package org.jetbrains.interpret

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import java.io.*
import java.util.HashMap
import java.lang.reflect.Constructor

trait Storage {
    fun get(property: String): Any
    fun set(property: String, value: Any)

    fun get(property: String, propertyType: Class<*>): Any
    fun set(property: String, propertyType: Class<*>, value: Any)
}

class UnsupportedPropertyTypeException(message: String) : Exception(message)

class StorageProperty<T>(val propertyType: Class<T>) {
    fun get(storage: Storage, property: String): T {
        return when (propertyType.getSimpleName()) {
            "Integer" -> storage.get(property)
            "Long" -> storage.get(property)
            "String" -> storage.get(property)
            "Short" -> storage.get(property)
            "Double" -> storage.get(property)
            "Byte" -> storage.get(property)
            "Character" -> storage.get(property)
            "Boolean" -> storage.get(property)
            else -> storage.get(property, propertyType)
        } as T
    }
    fun set(storage: Storage, property: String, value: T) {
        return when (propertyType.getSimpleName()) {
            "Integer" -> storage.set(property, value as Int)
            "Long" -> storage.set(property, value as Long)
            "Double" -> storage.set(property, value as Double)
            "Byte" -> storage.set(property, value as Byte)
            "String" -> storage.set(property, value as String)
            "Short" -> storage.set(property, value as Short)
            "Character" -> storage.set(property, value as Char)
            "Boolean" -> storage.set(property, value as Boolean)
            else -> storage.set(property, propertyType, value)
        }
    }
}

class EmitClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    public fun defineClass(name: String, b: ByteArray): Class<*>? {
        return defineClass(name, b, 0, b.size)
    }
}

class Emitter(val emitKlass: String, val protoName: String) : ClassVisitor(Opcodes.ASM4) {
    val writer = ClassWriter(0)
    val klassBuilder = writer// TraceClassVisitor(writer, ASMifier(), PrintWriter(System.out))
    val properties = HashMap<String, String>()
    val backendEntityKlass = javaClass<Storage>().getName().replace(".", "/")
    val backendPropertyKlass = javaClass<StorageProperty<*>>().getName().replace(".", "/")
    val backendKlass = javaClass<Storage>().getName().replace(".", "/")
    val backendPropertyType = Type.getObjectType(backendPropertyKlass)!!.getDescriptor()!!

    private fun getBoxedType(klass: Type): Type =
            when (klass.getSort()) {
                Type.BYTE -> Type.getObjectType("java/lang/Byte")!!
                Type.BOOLEAN -> Type.getObjectType("java/lang/Boolean")!!
                Type.SHORT -> Type.getObjectType("java/lang/Short")!!
                Type.CHAR -> Type.getObjectType("java/lang/Character")!!
                Type.INT -> Type.getObjectType("java/lang/Integer")!!
                Type.FLOAT -> Type.getObjectType("java/lang/Float")!!
                Type.LONG -> Type.getObjectType("java/lang/Long")!!
                Type.DOUBLE -> Type.getObjectType("java/lang/Double")!!
                else -> klass
            }

    fun setProperty(propertyName: String, propertyType: String) {
        val asmType = Type.getType(propertyType)
        val lowerPropertyName = propertyName.decapitalize()

        properties.put(lowerPropertyName, propertyType)

        // add getter
        val setter = klassBuilder.visitMethod(ACC_PUBLIC, "set$propertyName", "($propertyType)V", null, null)!!
        setter.visitCode()
        setter.visitVarInsn(ALOAD, 0)
        setter.visitFieldInsn(Opcodes.GETFIELD, emitKlass, "$lowerPropertyName\$delegate", "L$backendPropertyKlass;")
        setter.visitVarInsn(ALOAD, 0)
        setter.visitLdcInsn("$lowerPropertyName")
        when (propertyType) {
            "I" -> {
                setter.visitVarInsn(ILOAD, 1)
                setter.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;")
            }
            "Z" -> {
                setter.visitVarInsn(ILOAD, 1)
                setter.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;")
            }
            "J" -> {
                setter.visitVarInsn(LLOAD, 1)
                setter.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;")
            }
            "D" -> {
                setter.visitVarInsn(DLOAD, 1)
                setter.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;")
            }
            else -> {
                setter.visitVarInsn(ALOAD, 1)
            }
        }

        setter.visitMethodInsn(INVOKEVIRTUAL, backendPropertyKlass, "set", "(L$backendKlass;Ljava/lang/String;Ljava/lang/Object;)V")
        setter.visitInsn(RETURN)
        setter.visitMaxs(5, 3)
        setter.visitEnd()

    }

    fun getProperty(propertyName: String, propertyType: String) {
        val asmType = Type.getType(propertyType)
        val lowerPropertyName = propertyName.decapitalize()

        properties.put(lowerPropertyName, propertyType)

        // add getter
        val getter = klassBuilder.visitMethod(ACC_PUBLIC, "get$propertyName", "()$propertyType", null, null)!!
        getter.visitCode()
        getter.visitVarInsn(Opcodes.ALOAD, 0)
        getter.visitFieldInsn(Opcodes.GETFIELD, emitKlass, "$lowerPropertyName\$delegate", "L$backendPropertyKlass;")
        getter.visitVarInsn(Opcodes.ALOAD, 0)
        getter.visitLdcInsn("$lowerPropertyName")
        getter.visitMethodInsn(Opcodes.INVOKEVIRTUAL, backendPropertyKlass, "get", "(L$backendKlass;Ljava/lang/String;)Ljava/lang/Object;")
        when (propertyType) {
            "I" -> {
                getter.visitTypeInsn(CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I")
                getter.visitInsn(IRETURN)
            }
            "Z" -> {
                getter.visitTypeInsn(CHECKCAST, "java/lang/Boolean")
                getter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z")
                getter.visitInsn(IRETURN)
            }
            "J" -> {
                getter.visitTypeInsn(CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J")
                getter.visitInsn(LRETURN)
            }
            "D" -> {
                getter.visitTypeInsn(CHECKCAST, "java/lang/Number")
                getter.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D")
                getter.visitInsn(DRETURN)
            }
            else -> {
                getter.visitTypeInsn(Opcodes.CHECKCAST, "${asmType!!.getInternalName()}")
                getter.visitInsn(Opcodes.ARETURN)
            }
        }
        getter.visitMaxs(5, 1)
        getter.visitEnd()
    }

    override fun visitEnd() {
        // add fields
        for ((lowerPropertyName, propertyType) in properties) {
            val backingField = klassBuilder.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL, "$lowerPropertyName\$delegate", backendPropertyType,
                                                       "L$backendPropertyKlass<$propertyType>;", null)!!
            backingField.visitEnd()
        }

        // add constructor
        val ctor = klassBuilder.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L$backendEntityKlass;)V", null, null)!!
        ctor.visitCode()
        ctor.visitVarInsn(ALOAD, 1)
        ctor.visitLdcInsn("entity")
        ctor.visitMethodInsn(INVOKESTATIC, "kotlin/jvm/internal/Intrinsics", "checkParameterIsNotNull", "(Ljava/lang/Object;Ljava/lang/String;)V")
        ctor.visitVarInsn(ALOAD, 0)
        ctor.visitVarInsn(ALOAD, 1)
        ctor.visitMethodInsn(INVOKESPECIAL, backendKlass, "<init>", "(L$backendEntityKlass;)V")

        for ((lowerPropertyName, propertyType) in properties) {
            ctor.visitVarInsn(ALOAD, 0)
            ctor.visitTypeInsn(Opcodes.NEW, backendPropertyKlass)
            ctor.visitInsn(Opcodes.DUP)
            ctor.visitLdcInsn(getBoxedType(Type.getType(propertyType)!!))
            ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, backendPropertyKlass, "<init>", "(Ljava/lang/Class;)V")
            ctor.visitFieldInsn(Opcodes.PUTFIELD, emitKlass, "$lowerPropertyName\$delegate", backendPropertyType)
        }
        ctor.visitInsn(RETURN)
        ctor.visitMaxs(5, 3)
        ctor.visitEnd()

        klassBuilder.visitEnd()
        super<ClassVisitor>.visitEnd()
    }

    override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?) {
        klassBuilder.visit(V1_7, ACC_FINAL + ACC_SUPER, emitKlass, null, javaClass<Storage>().getName().replace(".", "/"), array(protoName))
        super<ClassVisitor>.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val name = name!!
        val desc = desc!!
        if (name.startsWith("get"))
            getProperty(name.drop(3), desc.dropWhile { it == '(' || it == ')' })
        else if (name.startsWith("set"))
            setProperty(name.drop(3), desc.dropWhile { it == '(' }.takeWhile { it != ')' })
        return super<ClassVisitor>.visitMethod(access, name, desc, signature, exceptions)
    }
    fun getBytes(): ByteArray = writer.toByteArray()!!
}

fun Storage.interpretAs<T>(klass: Class<T>): T {
    return emitWrapper(klass).newInstance(this) as T
}

fun Iterable<Storage>.interpretAs<T>(klass: Class<T>): Iterable<T> {
    val wrapper = emitWrapper(klass)
    return map { wrapper.newInstance(it) as T }
}

val emittedWrappers = HashMap<Class<*>, Constructor<*>>()
fun emitWrapper<T>(klass: Class<T>): Constructor<*> {
    val existingCtor = emittedWrappers.get(klass)
    if (existingCtor != null) {
        return existingCtor
    }

    val protoName = klass.getName()
    val emitName = protoName + "\$storage"
    val classFile = klass.getName().replace('.', '/') + ".class"
    val parentClassLoader = klass.getClassLoader()!!
    val reader = ClassReader(parentClassLoader.getResourceAsStream(classFile)!!)
    val emitter = Emitter(emitName.replace(".", "/"), protoName.replace(".", "/"))
    reader.accept(emitter, 0)
    val classLoader = EmitClassLoader(parentClassLoader)
    val emitKlass = classLoader.defineClass(emitName, emitter.getBytes())!!
    val ctor = emitKlass.getConstructor(javaClass<Storage>())
    ctor.setAccessible(true)
    emittedWrappers.put(klass, ctor)
    return ctor
}

/*
fun dump(prototype: Class<*>) {
    val cr = ClassReader(prototype.getName())
    val cv = TraceClassVisitor(null, ASMifier(), PrintWriter(System.out))
    cr.accept(cv, 0)
}
*/