package moe.nea.lisp

sealed class LispData {

    object LispNil : LispData()
    data class Atom(val label: String) : LispData()
    data class LispString(val string: String) : LispData()
    data class LispNumber(val value: Double) : LispData()
    data class LispNode(val node: LispAst.LispNode) : LispData()
    sealed class LispExecutable() : LispData() {
        abstract fun execute(
            executionContext: LispExecutionContext,
            callsite: LispAst.LispNode,
            stackFrame: StackFrame,
            args: List<LispAst.LispNode>
        ): LispData
    }

    abstract class JavaExecutable : LispExecutable()

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
                return executionContext.reportError(
                    "Expected ${argNames.size} arguments, got ${args.size} instead",
                    callsite
                )
            }
            val invocationFrame = declarationStackFrame.fork()

            for ((name, value) in argNames.zip(args)) {
                invocationFrame.setValueLocal(name, executionContext.resolveValue(stackFrame, value))
            }
            return executionContext.executeLisp(invocationFrame, body)
        }
    }


    companion object {
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
