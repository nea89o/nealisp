package moe.nea.lisp

object CoreBindings {
    val nil = LispData.LispNil
    val def = LispData.externalRawCall { context, callsite, stackFrame, args ->
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

    val pure = LispData.externalCall { args, reportError ->
        return@externalCall args.singleOrNull()?.let { value ->
            LispData.externalCall { args, reportError ->
                if (args.isNotEmpty())
                    reportError("Pure function does not expect arguments")
                else
                    value
            }
        } ?: reportError("Function pure expects exactly one argument")
    }

    val lambda = LispData.externalRawCall { context, callsite, stackFrame, args ->
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

    val defun = LispData.externalRawCall { context, callSite, stackFrame, lispAsts ->
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
    val seq = LispData.externalRawCall { context, callsite, stackFrame, args ->
        var lastResult: LispData? = null
        for (arg in args) {
            lastResult = context.executeLisp(stackFrame, arg)
        }
        lastResult ?: context.reportError("Seq cannot be invoked with 0 argumens", callsite)
    }

    val debuglog = LispData.externalRawCall { context, callsite, stackFrame, args ->
        println(args.joinToString(" ") { arg ->
            when (val resolved = context.resolveValue(stackFrame, arg)) {
                is LispData.Atom -> ":${resolved.label}"
                is LispData.JavaExecutable -> "<native code>"
                LispData.LispNil -> "nil"
                is LispData.LispNode -> resolved.node.toSource()
                is LispData.LispString -> resolved.string
                is LispData.LispNumber -> resolved.value.toString()
                is LispData.LispInterpretedCallable -> "<function ${resolved.name ?: "<anonymous>"} ${resolved.argNames} ${resolved.body.toSource()}>"
            }
        })
        LispData.LispNil
    }
    val add = LispData.externalCall { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call add without at least 1 argument")
        }
        LispData.LispNumber(args.fold(0.0) { a, b ->
            a + (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val sub = LispData.externalCall { args, reportError ->
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
    val mul = LispData.externalCall { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call mul without at least 1 argument")
        }
        LispData.LispNumber(args.fold(1.0) { a, b ->
            a * (b as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $b, expected number")).value
        })
    }
    val div = LispData.externalCall { args, reportError ->
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

    fun offerArithmeticTo(bindings: StackFrame) {
        bindings.setValueLocal("+", add)
        bindings.setValueLocal("/", div)
        bindings.setValueLocal("*", mul)
        bindings.setValueLocal("-", sub)
    }

    fun offerAllTo(bindings: StackFrame) {
        bindings.setValueLocal("nil", nil)
        bindings.setValueLocal("def", def)
        bindings.setValueLocal("pure", pure)
        bindings.setValueLocal("lambda", lambda)
        bindings.setValueLocal("defun", defun)
        bindings.setValueLocal("seq", seq)
        bindings.setValueLocal("debuglog", debuglog)
        offerArithmeticTo(bindings)
    }
}