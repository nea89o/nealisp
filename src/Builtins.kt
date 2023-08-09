package moe.nea.lisp

object Builtins {
    val builtinSource = Builtins::class.java.getResourceAsStream("/builtins.lisp")!!.bufferedReader().readText()
    val builtinProgram = LispParser.parse("builtins.lisp", builtinSource)
    fun loadBuiltins(
        lispExecutionContext: LispExecutionContext,
        consumer: (String, LispData) -> Unit,
    ) {
        val stackFrame = lispExecutionContext.genBindings()
        stackFrame.setValueLocal("export", LispData.externalRawCall { context, callsite, stackFrame, args ->
            val (name) = args
            consumer((name as LispAst.Reference).label, context.resolveValue(stackFrame, name))
            return@externalRawCall LispData.LispNil
        })
        lispExecutionContext.executeProgram(stackFrame, builtinProgram)
    }
}