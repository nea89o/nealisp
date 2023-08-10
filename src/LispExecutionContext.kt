package moe.nea.lisp

class LispExecutionContext() {

    private val errorReporter = LispErrorReporter()
    val rootStackFrame = StackFrame(null)
    val unloadedModules = mutableMapOf<String, LispAst.Program>()
    val modules = mutableMapOf<String, Map<String, LispData>>()

    fun reportError(name: String, position: HasLispPosition): LispData.LispNil {
        println("Error: $name ${position.position}")
        return LispData.LispNil
    }


    fun genBindings(): StackFrame {
        return StackFrame(rootStackFrame)
    }

    fun setupStandardBindings() {
        CoreBindings.offerAllTo(rootStackFrame)
        registerModule("builtins", Builtins.builtinProgram)
        importModule("builtins", rootStackFrame, object : HasLispPosition {
            override val position: LispPosition
                get() = error("Builtin import failed")

        })
    }

    fun registerModule(moduleName: String, program: LispAst.Program) {
        if (moduleName in unloadedModules || moduleName in modules) {
            error("Cannot register already registered module $moduleName")
        }
        unloadedModules[moduleName] = program
    }

    fun importModule(moduleName: String, into: StackFrame, position: HasLispPosition) {
        var exports = modules[moduleName]
        if (exports == null) {
            val module = unloadedModules[moduleName]
            if (module == null) {
                reportError("Could not find module $moduleName", position)
                return
            }
            exports = realizeModule(moduleName)
        }
        into.variables.putAll(exports)
    }

    private fun realizeModule(moduleName: String): Map<String, LispData> {
        val map = mutableMapOf<String, LispData>()
        modules[moduleName] = map
        val module = unloadedModules.remove(moduleName) ?: error("Could not find module $moduleName")
        val stackFrame = genBindings()
        stackFrame.setValueLocal("export", LispData.externalRawCall { context, callsite, stackFrame, args ->
            args.forEach { name ->
                if (name !is LispAst.Reference) {
                    context.reportError("Invalid export", name)
                    return@forEach
                }
                map[name.label] = context.resolveValue(stackFrame, name)
            }
            return@externalRawCall LispData.LispNil
        })
        executeProgram(stackFrame, module)
        return map
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

