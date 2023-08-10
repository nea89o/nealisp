package moe.nea.lisp

object CoreBindings {
    val def = LispData.externalRawCall("def") { context, callsite, stackFrame, args ->
        if (args.size != 2) {
            return@externalRawCall context.reportError("Function define expects exactly two arguments", callsite)
        }
        val (name, value) = args
        if (name !is LispAst.Reference) {
            return@externalRawCall context.reportError("Define expects a name as first argument", name)
        }
        if (name.label in stackFrame.variables) {
            return@externalRawCall context.reportError("Cannot redefine value in local context", name)
        }
        return@externalRawCall stackFrame.setValueLocal(name.label, context.resolveValue(stackFrame, value))
    }

    private fun isTruthy(data: LispData): Boolean? {
        if (data == trueValue) return true
        if (data == falseValue) return false
        return null
    }

    val trueValue = LispData.Atom("true")
    val falseValue = LispData.Atom("false")

    val ifFun = LispData.externalRawCall("if") { context, callsite, stackFrame, args ->
        if (args.size != 3) {
            return@externalRawCall context.reportError("if requires 3 arguments", callsite)
        }
        val (cond, ifTrue, ifFalse) = args

        val c = isTruthy(context.resolveValue(stackFrame, cond))
        if (c == null) {
            return@externalRawCall context.reportError("Non boolean value $c used as condition for if", cond)
        }
        if (c) {
            return@externalRawCall context.resolveValue(stackFrame, ifTrue)
        } else {
            return@externalRawCall context.resolveValue(stackFrame, ifFalse)
        }
    }

    val pure = LispData.externalCall("pure") { args, reportError ->
        return@externalCall args.singleOrNull()?.let { value ->
            LispData.externalCall("pure.r") { args, reportError ->
                if (args.isNotEmpty())
                    reportError("Pure function does not expect arguments")
                else
                    value
            }
        } ?: reportError("Function pure expects exactly one argument")
    }

    val lambda = LispData.externalRawCall("lambda") { context, callsite, stackFrame, args ->
        if (args.size != 2) {
            return@externalRawCall context.reportError("Lambda needs exactly 2 arguments", callsite)
        }
        val (argumentNames, body) = args
        if (argumentNames !is LispAst.Parenthesis) {
            return@externalRawCall context.reportError("Lambda has invalid argument declaration", argumentNames)
        }
        val argumentNamesString = argumentNames.items.map {
            val ref = it as? LispAst.Reference
            if (ref == null) {
                return@externalRawCall context.reportError("Lambda has invalid argument declaration", it)
            }
            ref.label
        }
        if (body !is LispAst.Parenthesis) {
            return@externalRawCall context.reportError("Lambda has invalid body declaration", body)
        }
        LispData.createLambda(stackFrame, argumentNamesString, body)
    }

    val defun = LispData.externalRawCall("defun") { context, callSite, stackFrame, lispAsts ->
        if (lispAsts.size != 3) {
            return@externalRawCall context.reportError("Invalid function definition", callSite)
        }
        val (name, args, body) = lispAsts
        if (name !is LispAst.Reference) {
            return@externalRawCall context.reportError("Invalid function definition name", name)
        }
        if (name.label in stackFrame.variables) {
            return@externalRawCall context.reportError("Cannot redefine function in local context", name)
        }
        if (args !is LispAst.Parenthesis) {
            return@externalRawCall context.reportError("Invalid function definition arguments", args)
        }
        val argumentNames = args.items.map {
            val ref = it as? LispAst.Reference
                ?: return@externalRawCall context.reportError("Invalid function definition argument name", it)
            ref.label
        }
        if (body !is LispAst.Parenthesis) {
            return@externalRawCall context.reportError("Invalid function definition body", body)
        }
        return@externalRawCall stackFrame.setValueLocal(
            name.label,
            LispData.createLambda(stackFrame, argumentNames, body, name.label)
        )
    }
    val seq = LispData.externalRawCall("seq") { context, callsite, stackFrame, args ->
        var lastResult: LispData? = null
        for (arg in args) {
            lastResult = context.executeLisp(stackFrame, arg)
        }
        lastResult ?: context.reportError("Seq cannot be invoked with 0 argumens", callsite)
    }

    internal fun stringify(thing: LispData): String {
        return when (thing) {
            is LispData.Atom -> ":${thing.label}"
            is LispData.JavaExecutable -> "<native function ${thing.name}>"
            LispData.LispNil -> "nil"
            is LispData.LispNode -> thing.node.toSource()
            is LispData.LispList -> thing.elements.joinToString(", ", "[", "]") { stringify(it) }
            is LispData.LispString -> thing.string
            is LispData.LispNumber -> thing.value.toString()
            is LispData.LispInterpretedCallable -> "<function ${thing.name ?: "<anonymous>"} ${thing.argNames} ${thing.body.toSource()}>"
        }
    }

    val tostring = LispData.externalCall("tostring") { args, reportError ->
        LispData.LispString(args.joinToString(" ") { stringify(it) })
    }

    val debuglog = LispData.externalCall("debuglog") { args, reportError ->
        println(args.joinToString(" ") { stringify(it) })
        LispData.LispNil
    }
    val add = LispData.externalCall("add") { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call add without at least 1 argument")
        }
        LispData.LispNumber(args.fold(0.0) { a, b ->
            a + (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val sub = LispData.externalCall("sub") { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call sub without at least 1 argument")
        }
        val c = args.map {
            (it as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $it, expected number")
                    ).value
        }
        LispData.LispNumber(args.drop(1).fold(c.first()) { a, b ->
            a - (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val mul = LispData.externalCall("mul") { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call mul without at least 1 argument")
        }
        LispData.LispNumber(args.fold(1.0) { a, b ->
            a * (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val div = LispData.externalCall("div") { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call div without at least 1 argument")
        }
        val c = args.map {
            (it as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $it, expected number")
                    ).value
        }
        LispData.LispNumber(args.drop(1).fold(c.first()) { a, b ->
            a / (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val import = LispData.externalRawCall("import") { context, callsite, stackFrame, args ->
        if (args.size != 1) {
            return@externalRawCall context.reportError("import needs at least one argument", callsite)
        }
        // TODO: aliased / namespaced imports
        val moduleName = when (val moduleObject = context.resolveValue(stackFrame, args[0])) {
            is LispData.Atom -> moduleObject.label
            is LispData.LispString -> moduleObject.string
            else -> return@externalRawCall context.reportError("import needs a string or atom as argument", callsite)
        }
        context.importModule(moduleName, stackFrame, callsite)
        return@externalRawCall LispData.LispNil
    }

    fun offerArithmeticTo(bindings: StackFrame) {
        bindings.setValueLocal("+", add)
        bindings.setValueLocal("/", div)
        bindings.setValueLocal("*", mul)
        bindings.setValueLocal("-", sub)
    }

    fun offerAllTo(bindings: StackFrame) {
        bindings.setValueLocal("if", ifFun)
        bindings.setValueLocal("nil", LispData.LispNil)
        bindings.setValueLocal("def", def)
        bindings.setValueLocal("tostring", tostring)
        bindings.setValueLocal("pure", pure)
        bindings.setValueLocal("lambda", lambda)
        bindings.setValueLocal("defun", defun)
        bindings.setValueLocal("seq", seq)
        bindings.setValueLocal("import", import)
        bindings.setValueLocal("debuglog", debuglog)
        offerArithmeticTo(bindings)
    }
}