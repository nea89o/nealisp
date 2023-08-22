package moe.nea.lisp.bind

import moe.nea.lisp.*
import java.lang.invoke.MethodHandles
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Parameter

typealias ObjectMapper = (AutoBinder.ParameterRemappingContext) -> (Any?)

class AutoBinder {
    companion object {
        val Parameter.effectiveType get() = if (this.isVarArgs) this.type.componentType else this.type
    }

    private fun mapLispData(parameter: Parameter): ObjectMapper? {
        if (LispData::class.java.isAssignableFrom(parameter.effectiveType)) return {
            parameter.effectiveType.cast(
                it.getOne()
            )
        }
        return null
    }

    private fun mapForeignObject(parameter: Parameter): ObjectMapper? {
        parameter.getAnnotation(UnmapForeignObject::class.java) ?: return null
        return {
            when (val x = it.getOne()) {
                is LispData.ForeignObject<*> -> {
                    parameter.effectiveType.cast(x.obj)
                }

                else -> {
                    it.errorReporter.reportError("$x needs to be of type")
                    null
                }
            }
        }
    }

    private fun mapErrorReporter(parameter: Parameter): ObjectMapper? {
        if (ErrorReporter::class.java.isAssignableFrom(parameter.effectiveType)) return { it.errorReporter }
        return null
    }

    private fun mapString(parameter: Parameter): ObjectMapper? {
        if (String::class.java == parameter.effectiveType) return {
            when (val x = it.getOne()) {
                is LispData.LispString -> x.string
                is LispData.Atom -> x.label
                else -> null.also { _ -> it.errorReporter.reportError("Could not coerce $x to string") }
            }
        }
        return null
    }

    private fun mapBoolean(parameter: Parameter): ObjectMapper? {
        if (Boolean::class.java.isAssignableFrom(parameter.effectiveType)) return {
            val x = it.getOne()
            val y = CoreBindings.isTruthy(x)
            if (y == null) {
                it.errorReporter.reportError("Could not coerce $x to a boolean")
            }
            y
        }
        return null
    }

    private fun mapAST(parameter: Parameter): ObjectMapper? {
        if (LispAst::class.java.isAssignableFrom(parameter.effectiveType)) return {
            parameter.effectiveType.cast(
                it.getOneNode()
            )
        }
        return null
    }

    private fun mapNumber(parameter: Parameter): ObjectMapper? {
        if (Double::class.java.isAssignableFrom(parameter.effectiveType)) return {
            when (val x = it.getOne()) {
                is LispData.LispNumber -> x.value
                else -> null.also { _ -> it.errorReporter.reportError("Could not coerce $x to number") }
            }
        }
        if (Float::class.java.isAssignableFrom(parameter.effectiveType)) return {
            when (val x = it.getOne()) {
                is LispData.LispNumber -> x.value.toFloat()
                else -> null.also { _ -> it.errorReporter.reportError("Could not coerce $x to number") }
            }
        }
        if (Int::class.java.isAssignableFrom(parameter.effectiveType)) return {
            when (val x = it.getOne()) {
                is LispData.LispNumber -> x.value.toInt()
                else -> null.also { _ -> it.errorReporter.reportError("Could not coerce $x to number") }
            }
        }
        if (Long::class.java.isAssignableFrom(parameter.effectiveType)) return {
            when (val x = it.getOne()) {
                is LispData.LispNumber -> x.value.toLong()
                else -> null.also { _ -> it.errorReporter.reportError("Could not coerce $x to number") }
            }
        }
        return null
    }

    private fun mapStackFrame(parameter: Parameter): ObjectMapper? {
        if (parameter.effectiveType == StackFrame::class.java) return {
            it.stackFrame
        }
        if (parameter.effectiveType == LispExecutionContext::class.java) return {
            it.context
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
        ::mapStackFrame,
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
        return {
            val l = buildList {
                while (it.hasMore())
                    add(baseMapper(it)!!)
            }
            val a = java.lang.reflect.Array.newInstance(parameter.type.componentType, l.size) as Array<Any>
            l.withIndex().forEach { a[it.index] = it.value }
            a
        }
    }

    private val lookup = MethodHandles.publicLookup()

    data class ParameterRemappingContext(
        val getOne: () -> LispData,
        val getOneNode: () -> LispAst.LispNode,
        val hasMore: () -> Boolean,
        val errorReporter: ErrorReporter,
        val stackFrame: StackFrame,
        val context: LispExecutionContext,
        val callsite: LispAst.LispNode,
        val args: List<LispAst.LispNode>,
    )

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
                val prc = ParameterRemappingContext(
                    { context.resolveValue(stackFrame, iterator.next()) },
                    { iterator.next() },
                    { iterator.hasNext() },
                    e,
                    stackFrame,
                    context,
                    callsite,
                    args
                )
                val p = objectMappers.map {
                    it.invoke(prc)
                        ?: return@externalRawCall LispData.LispNil
                }
                if (iterator.hasNext()) return@externalRawCall e.reportError("Too many arguments")
                val r = mh.invokeWithArguments(p)
                if (method.returnType == Void.TYPE || method.returnType == Unit::class.java)
                    return@externalRawCall LispData.LispNil
                r as LispData
            } catch (x: Exception) {
                e.reportError("$name threw an exception", x)
            }
        }
    }

    fun bindTo(obj: Any, frame: StackFrame) {
        frame.variables.putAll(generateInstanceBindings(obj))
    }
}