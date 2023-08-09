package moe.nea.lisp

import java.io.Reader
import javax.script.AbstractScriptEngine
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.SimpleBindings

class LispScriptEngine(private val factory: LispScriptEngineFactory) : AbstractScriptEngine() {
    val executionContext = LispExecutionContext()
    override fun eval(script: String, context: ScriptContext): LispData? {
        val fileName = context.getAttribute("scriptName") as? String ?: "script.lisp"
        val program = LispParser.parse(fileName, script)
        val root = executionContext.genBindings()
        CoreBindings.offerAllTo(root)
        for ((name, value) in context.getBindings(ScriptContext.ENGINE_SCOPE)) {
            when (value) {
                is String -> root.setValueLocal(name, LispData.LispString(value))
                is Number -> root.setValueLocal(name, LispData.LispNumber(value.toDouble()))
                is Boolean -> root.setValueLocal(name, LispData.LispNumber(if (value) 1.0 else 0.0))
                null -> root.setValueLocal(name, LispData.LispNil)
                else -> error("Could not convert $value to lisp value")
            }
        }
        return executionContext.executeProgram(root, program)
    }

    override fun eval(reader: Reader, context: ScriptContext): LispData? {
        return eval(reader.readText(), context)
    }

    override fun createBindings(): Bindings {
        return SimpleBindings()
    }

    override fun getFactory(): LispScriptEngineFactory {
        return factory
    }
}