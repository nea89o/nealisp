package moe.nea.lisp

object Builtins {

    private fun builtin(name: String) =
        LispParser.parse(
            "$name.lisp",
            Builtins::class.java.getResourceAsStream("/$name.lisp")!!.bufferedReader().readText()
        )

    val builtinProgram = builtin("builtins")
    val testProgram = builtin("stdtest")
}