package moe.nea.lisp

class LispExecutionContext() {

    private val errorReporter = LispErrorReporter()
    private val rootStackFrame = StackFrame(null)


    fun reportError(name: String, position: HasLispPosition): LispData.LispNil {
        println("Error: $name ${position.position}")
        return LispData.LispNil
    }


    fun genBindings(): StackFrame {
        return StackFrame(rootStackFrame)
    }

    fun executeProgram(stackFrame: StackFrame, program: LispAst.Program): LispData? {
        var lastValue: LispData? = null
        for (node in program.nodes) {
            lastValue = executeLisp(stackFrame, node)
        }
        return lastValue
    }


    fun executeLisp(stackFrame: StackFrame, node: LispAst.LispNode): LispData {
        when (node) {
            is LispAst.Parenthesis -> {
                val first = node.items.firstOrNull()
                    ?: return reportError("Cannot execute empty parenthesis ()", node)

                val rest = node.items.drop(1)
                return when (val resolvedValue = resolveValue(stackFrame, first)) {
                    is LispData.LispExecutable -> {
                        resolvedValue.execute(this, node, stackFrame, rest)
                    }

                    else -> reportError("Cannot evaluate expression of type $resolvedValue", node)
                }

            }

            else -> return reportError("Expected invocation", node)
        }
    }

    fun resolveValue(stackFrame: StackFrame, node: LispAst.LispNode): LispData {
        return when (node) {
            is LispAst.Atom -> LispData.Atom(node.label)
            is LispAst.Parenthesis -> executeLisp(stackFrame, node)
            is LispAst.Reference -> stackFrame.resolveReference(node.label)
                ?: reportError("Could not resolve variable ${node.label}", node)
            is LispAst.NumberLiteral -> LispData.LispNumber(node.numberValue)
            is LispAst.StringLiteral -> LispData.LispString(node.parsedString)
        }
    }
}

