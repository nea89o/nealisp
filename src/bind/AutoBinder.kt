package moe.nea.lisp.bind

import moe.nea.lisp.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

typealias ObjectMapper = (() -> LispData, () -> LispAst, ErrorReporter, () -> Boolean) -> (Any?)

class AutoBinder {
    companion object {
        val Parameter.effectiveType get() = if (this.isVarArgs) this.type.componentType else this.type
    }

    private fun mapLispData(parameter: Parameter): ObjectMapper? {
        if (LispData::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            parameter.effectiveType.cast(
                a()
            )
        }
        return null
    }

    private fun mapForeignObject(parameter: Parameter): ObjectMapper? {
        parameter.getAnnotation(UnmapForeignObject::class.java) ?: return null
        return { a, b, c, d ->
            when (val x = a()) {
                is LispData.ForeignObject<*> -> {
                    parameter.effectiveType.cast(x.obj)
                }

                else -> {
                    c.reportError("$x needs to be of type")
                    null
                }
            }
        }
    }

    private fun mapErrorReporter(parameter: Parameter): ObjectMapper? {
        if (ErrorReporter::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d -> c }
        return null
    }

    private fun mapString(parameter: Parameter): ObjectMapper? {
        if (String::class.java == parameter.effectiveType) return { a, b, c, d ->
            when (val x = a()) {
                is LispData.LispString -> x.string
                is LispData.Atom -> x.label
                else -> null.also { c.reportError("Could not coerce $x to string") }
            }
        }
        return null
    }

    private fun mapBoolean(parameter: Parameter): ObjectMapper? {
        if (Boolean::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            val x = a()
            val y = CoreBindings.isTruthy(x)
            if (y == null) {
                c.reportError("Could not coerce $x to a boolean")
            }
            y
        }
        return null
    }

    private fun mapAST(parameter: Parameter): ObjectMapper? {
        if (LispAst::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            parameter.effectiveType.cast(
                b()
            )
        }
        return null
    }

    private fun mapNumber(parameter: Parameter): ObjectMapper? {
        if (Double::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Float::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toFloat()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Int::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toInt()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        if (Long::class.java.isAssignableFrom(parameter.effectiveType)) return { a, b, c, d ->
            when (val x = a()) {
                is LispData.LispNumber -> x.value.toLong()
                else -> null.also { c.reportError("Could not coerce $x to number") }
            }
        }
        return null
    }


    val objectMappers = mutableListOf<((Parameter) -> ObjectMapper?)>(
        ::mapLispData,
        ::mapErrorReporter,
        ::mapNumber,
        ::mapString,
        ::mapBoolean,
        ::mapAST,
        ::mapForeignObject,
    )


    fun generateInstanceBindings(obj: Any): Map<String, LispData> {
        val bindings = mutableMapOf<String, LispData>()
        for (method in obj.javaClass.methods) {
            val annotation = method.getAnnotation(LispBinding::class.java) ?: continue
            require(LispParser.isValidIdentifier(annotation.name))
            bindings[annotation.name] = wrapMethod(obj, annotation.name, method)
        }
        // TODO: field bindings
        return bindings
    }

    open fun makeVarArgMapper(
        parameter: Parameter,
        baseMapper: ObjectMapper
    ): ObjectMapper? {
        return { a, b, c, d ->
            val l = buildList {
                while (d())
                    add(baseMapper(a, b, c, d)!!)
            }
            val a = java.lang.reflect.Array.newInstance(parameter.type.componentType, l.size) as Array<Any>
            l.withIndex().forEach { a[it.index] = it.value }
            a
        }
    }

    private val lookup = MethodHandles.publicLookup()
    fun wrapMethod(obj: Any, name: String, method: Method): LispData.LispExecutable {
        var mh = lookup.unreflect(method)
        if (method.modifiers and Modifier.STATIC == 0) {
            mh = mh.bindTo(obj)
        }
        val objectMappers = method.parameters.map { param ->
            val baseMapper = objectMappers.firstNotNullOfOrNull { it.invoke(param) }
                ?: error("Could not find object mapper for parameter $param")
            if (param.isVarArgs) {
                makeVarArgMapper(param, baseMapper)
                    ?: error("Could not transform object mapper to vararg object mapper")
            } else
                baseMapper
        }
        return LispData.externalRawCall(name) { context: LispExecutionContext, callsite: LispAst.LispNode, stackFrame: StackFrame, args: List<LispAst.LispNode> ->
            val e = object : ErrorReporter {
                override fun reportError(string: String): LispData {
                    return stackFrame.reportError(string, callsite)
                }
            }
            try {
                val iterator = args.iterator()
                val (a, b, c) = Triple({ context.resolveValue(stackFrame, iterator.next()) }, { iterator.next() }, e)
                val p = objectMappers.map {
                    it.invoke(a, b, c, { iterator.hasNext() })
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