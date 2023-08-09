package moe.nea.lisp

sealed class LispData {

    fun <T : Any> lispCastObject(lClass: LispClass<T>): LispObject<T>? {
        if (this !is LispObject<*>) return null
        if (this.handler != lClass) return null
        return this as LispObject<T>
    }

    object LispNil : LispData()
    data class Atom(val label: String) : LispData()
    data class LispNode(val node: LispAst.LispNode) : LispData()
    data class LispNumber(val number: Double) : LispData()
    data class LispObject<T : Any>(val data: T, val handler: LispClass<T>) : LispData()
    sealed class LispExecutable() : LispData() {
        abstract fun execute(
            executionContext: LispExecutionContext,
            callsite: LispAst.LispNode,
            stackFrame: StackFrame,
            args: List<LispAst.LispNode>
        ): LispData
    }


    abstract class JavaExecutable : LispExecutable() {
    }

    data class LispInterpretedCallable(
        val declarationStackFrame: StackFrame,
        val argNames: List<String>,
        val body: LispAst.Parenthesis,
        val name: String?,
    ) : LispExecutable() {
        override fun execute(
            executionContext: LispExecutionContext,
            callsite: LispAst.LispNode,
            stackFrame: StackFrame,
            args: List<LispAst.LispNode>
        ): LispData {
            if (argNames.size != args.size) {
                TODO("ERROR")
            }
            val invocationFrame = declarationStackFrame.fork()

            for ((name, value) in argNames.zip(args)) {
                invocationFrame.setValueLocal(name, executionContext.resolveValue(stackFrame, value))
            }
            return executionContext.executeLisp(invocationFrame, body)
        }
    }

    interface LispClass<T : Any> {
        fun access(obj: T, name: String): LispData
        fun instantiate(obj: T) = LispObject(obj, this)
    }

    object LispStringClass : LispClass<String> {
        override fun access(obj: String, name: String): LispData {
            return LispNil
        }
    }

    companion object {
        fun string(value: String): LispObject<String> =
            LispStringClass.instantiate(value)

        fun externalRawCall(callable: (context: LispExecutionContext, callsite: LispAst.LispNode, stackFrame: StackFrame, args: List<LispAst.LispNode>) -> LispData): LispExecutable {
            return object : JavaExecutable() {
                override fun execute(
                    executionContext: LispExecutionContext,
                    callsite: LispAst.LispNode,
                    stackFrame: StackFrame,
                    args: List<LispAst.LispNode>
                ): LispData {
                    return callable.invoke(executionContext, callsite, stackFrame, args)
                }
            }
        }

        fun externalCall(callable: (args: List<LispData>, reportError: (String) -> LispData) -> LispData): LispExecutable {
            return object : JavaExecutable() {
                override fun execute(
                    executionContext: LispExecutionContext,
                    callsite: LispAst.LispNode,
                    stackFrame: StackFrame,
                    args: List<LispAst.LispNode>
                ): LispData {
                    val mappedArgs = args.map { executionContext.resolveValue(stackFrame, it) }
                    return callable.invoke(mappedArgs) { executionContext.reportError(it, callsite) }
                }
            }
        }


        fun createLambda(
            declarationStackFrame: StackFrame,
            args: List<String>,
            body: LispAst.Parenthesis,
            nameHint: String? = null,
        ): LispExecutable {
            return LispInterpretedCallable(declarationStackFrame, args, body, nameHint)
        }
    }
}
