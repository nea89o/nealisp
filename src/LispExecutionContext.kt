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

    fun executeProgram(stackFrame: StackFrame, program: LispAst.Program) {
        for (node in program.nodes) {
            executeLisp(stackFrame, node)
        }
    }


    fun executeLisp(stackFrame: StackFrame, node: LispAst.LispNode): LispData {
        when (node) {
            is LispAst.Parenthesis -> {
                val first = node.items.firstOrNull()
                    ?: return reportError("Cannot execute empty parenthesis ()", node)

                val rest = node.items.drop(1)
                return when (val resolvedValue = resolveValue(stackFrame, first)) {
                    is LispData.Atom -> reportError("Cannot execute atom", node)
                    LispData.LispNil -> reportError("Cannot execute nil", node)
                    is LispData.LispNumber -> reportError("Cannot execute number", node)
                    is LispData.LispNode -> reportError("Cannot execute node", node)
                    is LispData.LispObject<*> -> reportError("Cannot execute object-value", node)
                    is LispData.LispExecutable -> {
                        resolvedValue.execute(this, node, stackFrame, rest)
                    }
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

            is LispAst.StringLiteral -> LispData.string(node.parsedString)
        }
    }
}

