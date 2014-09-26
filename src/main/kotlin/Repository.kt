package org.jetbrains.interpret

import java.util.HashMap
import java.lang.reflect.Constructor
import org.objectweb.asm.ClassReader

class EmitClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    public fun defineClass(name: String, b: ByteArray): Class<*>? {
        return defineClass(name, b, 0, b.size)
    }
}

val emittedWrappers = HashMap<Class<*>, Constructor<*>>()
fun emitWrapper(klass: Class<*>): Constructor<*> {
    val existingCtor = emittedWrappers.get(klass)
    if (existingCtor != null) {
        return existingCtor
    }

    val protoName = klass.getName()
    val emitName = protoName + "\$backend"
    val classFile = klass.getName().replace('.', '/') + ".class"
    val parentClassLoader = klass.getClassLoader()!!
    val reader = ClassReader(parentClassLoader.getResourceAsStream(classFile)!!)
    val emitter = BackendEmitter(emitName.replace(".", "/"), protoName.replace(".", "/"))
    reader.accept(emitter, 0)
    val classLoader = EmitClassLoader(parentClassLoader)
    val emitKlass = classLoader.defineClass(emitName, emitter.getBytes())!!
    val ctor = emitKlass.getConstructor(javaClass<Storage>())
    ctor.setAccessible(true)
    emittedWrappers.put(klass, ctor)
    return ctor
}

