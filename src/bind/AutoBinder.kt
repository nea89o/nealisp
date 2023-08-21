package moe.nea.lisp.bind

import moe.nea.lisp.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

class AutoBinder {

    private fun mapLispData(parameter: Parameter): ((Iterator<LispData>, ErrorReporter) -> Any)? {
        if (LispData::class.java.isAssignableFrom(parameter.type)) return { a, b -> parameter.type.cast(a.next()) }
        return null
    }

    private fun mapErrorReporter(parameter: Parameter): ((Iterator<LispData>, ErrorReporter) -> Any)? {
        if (ErrorReporter::class.java.isAssignableFrom(parameter.type)) return { a, b -> b }
        return null
    }

    private fun mapString(parameter: Parameter): ((Iterator<LispData>, ErrorReporter) -> Any?)? {
        if (String::class.java == parameter.type) return { a, b ->
            when (val x = a.next()) {
                is LispData.LispString -> x.string
                is LispData.Atom -> x.label
                else -> null.also { b.reportError("Could not coerce $x to string") }
            }
        }
        return null
    }

    private fun mapBoolean(parameter: Parameter): ((Iterator<LispData>, ErrorReporter) -> Any?)? {
        if (Boolean::class.java.isAssignableFrom(parameter.type)) return { a, b ->
            val x = a.next()
            val y = CoreBindings.isTruthy(x)
            if (y == null) {
                b.reportError("Could not coerce $x to a boolean")
            }
            y
        }
        return null
    }

    private fun mapNumber(parameter: Parameter): ((Iterator<LispData>, ErrorReporter) -> Any?)? {
        if (Double::class.java.isAssignableFrom(parameter.type)) return { a, b ->
            when (val x = a.next()) {
                is LispData.LispNumber -> x.value
                else -> null.also { b.reportError("Could not coerce $x to number") }
            }
        }
        if (Float::class.java.isAssignableFrom(parameter.type)) return { a, b ->
            when (val x = a.next()) {
                is LispData.LispNumber -> x.value.toFloat()
                else -> null.also { b.reportError("Could not coerce $x to number") }
            }
        }
        if (Int::class.java.isAssignableFrom(parameter.type)) return { a, b ->
            when (val x = a.next()) {
                is LispData.LispNumber -> x.value.toInt()
                else -> null.also { b.reportError("Could not coerce $x to number") }
            }
        }
        if (Long::class.java.isAssignableFrom(parameter.type)) return { a, b ->
            when (val x = a.next()) {
                is LispData.LispNumber -> x.value.toLong()
                else -> null.also { b.reportError("Could not coerce $x to number") }
            }
        }
        return null
    }


    val objectMappers = mutableListOf<((Parameter) -> (((Iterator<LispData>, ErrorReporter) -> Any?)?))>(
        ::mapLispData,
        ::mapErrorReporter,
        ::mapNumber,
        ::mapString,
        ::mapBoolean,
    )


    fun generateInstanceBindings(obj: Any): Map<String, LispData> {
        val bindings = mutableMapOf<String, LispData>()
        for (method in obj.javaClass.methods) {
            val annotation = method.getAnnotation(LispBinding::class.java)
            if (annotation == null) continue
            require(LispParser.isValidIdentifier(annotation.name))
            bindings[annotation.name] = wrapMethod(obj, annotation.name, method)
        }
        // TODO: field bindings
        return bindings
    }

    private val lookup = MethodHandles.publicLookup()
    fun wrapMethod(obj: Any, name: String, method: Method): LispData.LispExecutable {
        var mh = lookup.unreflect(method)
        if (method.modifiers and Modifier.STATIC == 0) {
            mh = mh.bindTo(obj)
        }
        val objectMappers = method.parameters.map { param ->
            objectMappers.firstNotNullOfOrNull { it.invoke(param) }
                ?: error("Could not find object mapper for parameter $param")
        }
        return LispData.externalCall(name) { args, fReportError ->
            try {
                val iterator = args.iterator()
                val e = object : ErrorReporter {
                    override fun reportError(string: String): LispData {
                        return fReportError(string)
                    }
                }
                val p = objectMappers.map { it.invoke(iterator, e) ?: return@externalCall LispData.LispNil }
                if (iterator.hasNext()) return@externalCall fReportError("Too many arguments")
                mh.invokeWithArguments(p) as LispData
            } catch (e: Exception) {
                fReportError("$name threw an exception: $e")
            }
        }
    }

    fun bindTo(obj: Any, frame: StackFrame) {
        frame.variables.putAll(generateInstanceBindings(obj))
    }
}