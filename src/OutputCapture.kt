package moe.nea.lisp

object OutputCapture {
    data class CapturedOutput(
        internal var string: StringBuilder,
    ) {
        val asString get() = string.toString()
    }

    object Meta : StackFrame.MetaKey<CapturedOutput>

    fun captureOutput(stackFrame: StackFrame): CapturedOutput {
        val output = CapturedOutput(StringBuilder())
        stackFrame.setMeta(Meta, output)
        return output
    }

    fun print(stackFrame: StackFrame, text: String) {
        val output = stackFrame.getMeta(Meta)
        output?.string?.append(text)
        print(text)
    }
}