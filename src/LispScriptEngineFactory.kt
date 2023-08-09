package moe.nea.lisp

import javax.script.ScriptEngine
import javax.script.ScriptEngineFactory

class LispScriptEngineFactory : ScriptEngineFactory {
    override fun getEngineName(): String = "nealisp"

    override fun getEngineVersion(): String = "1.0.0"

    override fun getExtensions(): List<String> = listOf("lsp", "lisp", "nealisp")

    override fun getMimeTypes(): List<String> = listOf("application/x-lisp", "application/x-nealisp")

    override fun getNames(): List<String> = listOf("NeaLisp", "Lisp")

    override fun getLanguageName(): String = "Lisp"

    override fun getLanguageVersion(): String = "1.0.0"

    override fun getParameter(key: String?): String? {
        return when (key) {
            ScriptEngine.NAME -> languageName
            ScriptEngine.ENGINE_VERSION -> engineVersion
            ScriptEngine.ENGINE -> engineName
            ScriptEngine.LANGUAGE -> languageName
            ScriptEngine.LANGUAGE_VERSION -> languageVersion
            else -> null
        }
    }

    override fun getMethodCallSyntax(obj: String?, m: String?, vararg args: String?): String {
        return "($m $obj ${args.joinToString(" ")})"
    }

    override fun getOutputStatement(toDisplay: String?): String {
        return "(print $toDisplay)"
    }

    override fun getProgram(vararg statements: String?): String {
        return statements.joinToString("\n")
    }

    override fun getScriptEngine(): LispScriptEngine {
        return LispScriptEngine(this)
    }
}