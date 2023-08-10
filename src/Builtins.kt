package moe.nea.lisp

object Builtins {
    val builtinSource = Builtins::class.java.getResourceAsStream("/builtins.lisp")!!.bufferedReader().readText()
    val builtinProgram = LispParser.parse("builtins.lisp", builtinSource)
}