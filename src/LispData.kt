package moe.nea.lisp

sealed class LispData {

    object LispNil : LispData()
    data class Atom(val label: String) : LispData()
    data class LispString(val string: String) : LispData()
    data class LispNumber(val value: Double) : LispData()
    data class LispNode(val node: LispAst.LispNode) : LispData()
    data class LispHash(val map: Map<String, LispData>) : LispData()
    data class ForeignObject<T : Any?>(val obj: T) : LispData()
    class LispList(val elements: List<LispData>) : LispData()
    sealed class LispExecutable() : LispData() {
        abstract fun execute(
            executionContext: LispExecutionContext,
            callsite: LispAst.LispNode,
            stackFrame: StackFrame,
            args: List<LispAst.LispNode>
        ): LispData
    }

    abstract class JavaExecutable(val name: String) : LispExecutable()

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

            val invocationFrame = declarationStackFrame.fork()
            if (argNames.lastOrNull() == "...") {
                for ((name, value) in argNames.dropLast(1).zip(args)) {
                    invocationFrame.setValueLocal(name, executionContext.resolveValue(stackFrame, value))
                }
                invocationFrame.setValueLocal(
                    "...",
                    LispList(
                        args.drop(argNames.size - 1).map { executionContext.resolveValue(stackFrame, it) })
                )
            } else if (argNames.size != args.size) {
                return stackFrame.reportError(
                    "Expected ${argNames.size} arguments, got ${args.size} instead",
                    callsite
                )
            } else
                for ((name, value) in argNames.zip(args)) {
                    invocationFrame.setValueLocal(name, executionContext.resolveValue(stackFrame, value))
                }
            return executionContext.executeLisp(invocationFrame, body)
        }
    }

    companion object {
        fun externalRawCall(
            name: String,
            callable: (context: LispExecutionContext, callsite: LispAst.LispNode, stackFrame: StackFrame, args: List<LispAst.LispNode>) -> LispData
        ): LispExecutable {
            return object : JavaExecutable(name) {
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

        fun externalCall(
            name: String,
            callable: (args: List<LispData>, reportError: (String) -> LispData) -> LispData
        ): LispExecutable {
            return object : JavaExecutable(name) {
                override fun execute(
                    executionContext: LispExecutionContext,
                    callsite: LispAst.LispNode,
                    stackFrame: StackFrame,
                    args: List<LispAst.LispNode>
                ): LispData {
                    val mappedArgs = args.map { executionContext.resolveValue(stackFrame, it) }
                    return callable.invoke(mappedArgs) { stackFrame.reportError(it, callsite) }
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

        fun boolean(b: Boolean): Atom {
            return if (b) {
                CoreBindings.trueValue
            } else {
                CoreBindings.falseValue
            }
        }
    }
}
