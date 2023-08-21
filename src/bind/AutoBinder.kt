package moe.nea.lisp.bind

import moe.nea.lisp.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

class AutoBinder {

    private fun mapLispData(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any)? {
        if (LispData::class.java.isAssignableFrom(parameter.type)) return { a, b, c -> parameter.type.cast(a()) }
        return null
    }

    private fun mapErrorReporter(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any)? {
        if (ErrorReporter::class.java.isAssignableFrom(parameter.type)) return { a, b, c -> c }
        return null
    }

    private fun mapString(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any?)? {
        if (String::class.java == parameter.type) return { a, b, c ->
            when (val x = a()) {
                is LispData.LispString -> x.string
                is LispData.Atom -> x.label
                else -> null.also { c.reportError("Could not coerce $x to string") }
            }
        }
        return null
    }

    private fun mapBoolean(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any?)? {
        if (Boolean::class.java.isAssignableFrom(parameter.type)) return { a, b, c ->
            val x = a()
            val y = CoreBindings.isTruthy(x)
            if (y == null) {
                c.reportError("Could not coerce $x to a boolean")
            }
            y
        }
        return null
    }

    private fun mapAST(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any?)? {
        if (LispAst::class.java.isAssignableFrom(parameter.type)) return { a, b, c -> parameter.type.cast(b()) }
        return null
    }

    private fun mapNumber(parameter: Parameter): ((() -> LispData, () -> LispAst, ErrorReporter) -> Any?)? {
        if (Double::class.java.isAssignableFrom(parameter.type)) return { a, b, c ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Float::class.java.isAssignableFrom(parameter.type)) return { a, b, c ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toFloat()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Int::class.java.isAssignableFrom(parameter.type)) return { a, b, c ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toInt()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Long::class.java.isAssignableFrom(parameter.type)) return { a, b, c ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toLong()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        return null
    }


    val objectMappers = mutableListOf<((Parameter) -> (((() -> LispData, () -> LispAst, ErrorReporter) -> Any?)?))>(
        ::mapLispData,
        ::mapErrorReporter,
        ::mapNumber,
        ::mapString,
        ::mapBoolean,
        ::mapAST,
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
        return LispData.externalRawCall(name) { context: LispExecutionContext, callsite: LispAst.LispNode, stackFrame: StackFrame, args: List<LispAst.LispNode> ->
            val e = object : ErrorReporter {
                override fun reportError(string: String): LispData {
                    return stackFrame.reportError(string, callsite)
                }
            }
            try {
                val iterator = args.iterator()
                val p = objectMappers.map {
                    it.invoke({ context.resolveValue(stackFrame, iterator.next()) }, { iterator.next() }, e)
                        ?: return@externalRawCall LispData.LispNil
                }
                if (iterator.hasNext()) return@externalRawCall e.reportError("Too many arguments")
                mh.invokeWithArguments(p) as LispData
            } catch (x: Exception) {
                e.reportError("$name threw an exception", x)
            }
        }
    }

    fun bindTo(obj: Any, frame: StackFrame) {
        frame.variables.putAll(generateInstanceBindings(obj))
    }
}