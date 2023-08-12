package moe.nea.lisp

object CoreBindings {
    val def = LispData.externalRawCall("def") { context, callsite, stackFrame, args ->
        if (args.size != 2) {
            return@externalRawCall stackFrame.reportError("Function define expects exactly two arguments", callsite)
        }
        val (name, value) = args
        if (name !is LispAst.Reference) {
            return@externalRawCall stackFrame.reportError("Define expects a name as first argument", name)
        }
        if (name.label in stackFrame.variables) {
            return@externalRawCall stackFrame.reportError("Cannot redefine value in local context", name)
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
            return@externalRawCall stackFrame.reportError("if requires 3 arguments", callsite)
        }
        val (cond, ifTrue, ifFalse) = args

        val c = isTruthy(context.resolveValue(stackFrame, cond))
        if (c == null) {
            return@externalRawCall stackFrame.reportError("Non boolean value $c used as condition for if", cond)
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
            return@externalRawCall stackFrame.reportError("Lambda needs exactly 2 arguments", callsite)
        }
        val (argumentNames, body) = args
        if (argumentNames !is LispAst.Parenthesis) {
            return@externalRawCall stackFrame.reportError("Lambda has invalid argument declaration", argumentNames)
        }
        val argumentNamesString = argumentNames.items.map {
            val ref = it as? LispAst.Reference
            if (ref == null) {
                return@externalRawCall stackFrame.reportError("Lambda has invalid argument declaration", it)
            }
            ref.label
        }
        if (body !is LispAst.Parenthesis) {
            return@externalRawCall stackFrame.reportError("Lambda has invalid body declaration", body)
        }
        LispData.createLambda(stackFrame, argumentNamesString, body)
    }

    val defun = LispData.externalRawCall("defun") { context, callSite, stackFrame, lispAsts ->
        if (lispAsts.size != 3) {
            return@externalRawCall stackFrame.reportError("Invalid function definition", callSite)
        }
        val (name, args, body) = lispAsts
        if (name !is LispAst.Reference) {
            return@externalRawCall stackFrame.reportError("Invalid function definition name", name)
        }
        if (name.label in stackFrame.variables) {
            return@externalRawCall stackFrame.reportError("Cannot redefine function in local context", name)
        }
        if (args !is LispAst.Parenthesis) {
            return@externalRawCall stackFrame.reportError("Invalid function definition arguments", args)
        }
        val argumentNames = args.items.map {
            val ref = it as? LispAst.Reference
                ?: return@externalRawCall stackFrame.reportError("Invalid function definition argument name", it)
            ref.label
        }
        if (body !is LispAst.Parenthesis) {
            return@externalRawCall stackFrame.reportError("Invalid function definition body", body)
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
        lastResult ?: stackFrame.reportError("Seq cannot be invoked with 0 argumens", callsite)
    }

    internal fun stringify(thing: LispData): String {
        return when (thing) {
            is LispData.Atom -> ":${thing.label}"
            is LispData.JavaExecutable -> "<native function ${thing.name}>"
            LispData.LispNil -> "nil"
            is LispData.LispNode -> thing.node.toSource()
            is LispData.LispList -> thing.elements.joinToString(", ", "[", "]") { stringify(it) }
            is LispData.LispString -> thing.string
            is LispData.LispHash -> thing.map.asIterable().joinToString(", ", "{", "}") { it.key + ": " + it.value }
            is LispData.LispNumber -> thing.value.toString()
            is LispData.LispInterpretedCallable -> "<function ${thing.name ?: "<anonymous>"} ${thing.argNames} ${thing.body.toSource()}>"
        }
    }

    val tostring = LispData.externalCall("tostring") { args, reportError ->
        LispData.LispString(args.joinToString(" ") { stringify(it) })
    }

    val debuglog = LispData.externalRawCall("debuglog") { context, callsite, stackFrame, args ->
        OutputCapture.print(
            stackFrame,
            args.joinToString(" ", postfix = "\n") { stringify(context.resolveValue(stackFrame, it)) })
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
        LispData.LispNumber(c.drop(1).fold(c.first()) { a, b ->
            a - b
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
    val eq = LispData.externalCall("eq") { args, reportError ->
        if (args.size == 0) {
            return@externalCall reportError("Cannot call eq without at least 1 argument")
        }
        LispData.boolean(!args.zipWithNext().any { it.first != it.second })
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
        LispData.LispNumber(c.drop(1).fold(c.first()) { a, b ->
            a / b
        })
    }
    val less = LispData.externalCall("less") { args, reportError ->
        if (args.size != 2) {
            return@externalCall reportError("Cannot call less without exactly 2 arguments")
        }
        val (left, right) = args.map {
            (it as? LispData.LispNumber
                ?: return@externalCall reportError("Unexpected argument $it, expected number")
                    ).value
        }
        LispData.boolean(left < right)
    }

    private fun atomOrStringToString(thing: LispData): String? {
        return when (thing) {
            is LispData.Atom -> thing.label
            is LispData.LispString -> thing.string
            else -> null
        }
    }

    val import = LispData.externalRawCall("import") { context, callsite, stackFrame, args ->
        if (args.size != 1) {
            return@externalRawCall stackFrame.reportError("import needs at least one argument", callsite)
        }
        // TODO: aliased / namespaced imports
        val moduleName = atomOrStringToString(context.resolveValue(stackFrame, args[0]))
            ?: return@externalRawCall stackFrame.reportError("import needs a string or atom as argument", callsite)
        context.importModule(moduleName, stackFrame, callsite)
        return@externalRawCall LispData.LispNil
    }

    val reflect = LispData.externalCall("reflect.type") { args, reportError ->
        if (args.size != 1) {
            return@externalCall reportError("reflect.type can only return the type for one argument")
        }

        return@externalCall when (args[0]) {
            is LispData.Atom -> LispData.Atom("atom")
            is LispData.LispExecutable -> LispData.Atom("callable")
            is LispData.LispList -> LispData.Atom("list")
            LispData.LispNil -> LispData.Atom("nil")
            is LispData.LispHash -> LispData.Atom("hash")
            is LispData.LispNode -> LispData.Atom("ast")
            is LispData.LispNumber -> LispData.Atom("number")
            is LispData.LispString -> LispData.Atom("string")
        }
    }

    fun offerArithmeticTo(bindings: StackFrame) {
        bindings.setValueLocal("core.arith.add", add)
        bindings.setValueLocal("core.arith.div", div)
        bindings.setValueLocal("core.arith.mul", mul)
        bindings.setValueLocal("core.arith.sub", sub)
        bindings.setValueLocal("core.arith.less", less)
        bindings.setValueLocal("core.arith.eq", eq)
    }

    fun offerHashesTo(bindings: StackFrame) {
        bindings.setValueLocal("core.newhash", LispData.externalCall("newhash") { args, reportError ->
            if (args.size % 2 != 0) {
                return@externalCall reportError("Hash creation needs to have an even number of arguments")
            }

            LispData.LispHash(
                args.chunked(2).associate { (a, b) ->
                    (atomOrStringToString(a)
                        ?: return@externalCall reportError("Hash key needs to be string or atom")) to b
                })
        })
        bindings.setValueLocal("core.gethash", LispData.externalCall("gethash") { args, reportError ->
            if (args.size != 2) {
                return@externalCall reportError("Hash access needs 2 arguments")
            }
            val (hash, name) = args
            if (hash !is LispData.LispHash) {
                return@externalCall reportError("$hash is not a hash")
            }
            val nameS =
                atomOrStringToString(name) ?: return@externalCall reportError("$name is not an atom or a string")
            hash.map[nameS] ?: LispData.LispNil
        })
        bindings.setValueLocal("core.mergehash", LispData.externalCall("mergehash") { args, reportError ->
            val m = mutableMapOf<String, LispData>()
            for (arg in args) {
                if (arg !is LispData.LispHash) {
                    return@externalCall reportError("$arg is not a hash")
                }
                m.putAll(arg.map)
            }
            LispData.LispHash(m)
        })
    }

    fun offerAllTo(bindings: StackFrame) {
        bindings.setValueLocal("core.if", ifFun)
        bindings.setValueLocal("core.nil", LispData.LispNil)
        bindings.setValueLocal("core.def", def)
        bindings.setValueLocal("core.tostring", tostring)
        bindings.setValueLocal("core.pure", pure)
        bindings.setValueLocal("core.lambda", lambda)
        bindings.setValueLocal("core.defun", defun)
        bindings.setValueLocal("core.seq", seq)
        bindings.setValueLocal("core.import", import)
        bindings.setValueLocal("core.reflect.type", reflect)
        bindings.setValueLocal("core.debuglog", debuglog)
        offerArithmeticTo(bindings)
        offerHashesTo(bindings)
    }
}