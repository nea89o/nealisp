package moe.nea.lisp

class StackFrame(val parent: StackFrame?) {

    val variables = mutableMapOf<String, LispData>()

    interface MetaKey<T>

    private val meta: MutableMap<MetaKey<*>, Any> = mutableMapOf()

    fun <T : Any> getMeta(key: MetaKey<T>): T? {
        return meta[key] as? T
    }

    fun <T : Any> setMeta(key: MetaKey<T>, value: T) {
        meta[key] = value
    }

    fun resolveReference(label: String): LispData? =
        variables[label] ?: parent?.resolveReference(label)

    fun setValueLocal(label: String, value: LispData): LispData {
        variables[label] = value
        return value
    }

    fun fork(): StackFrame {
        return StackFrame(this)
    }


}
