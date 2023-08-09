package moe.nea.lisp

class LispErrorReporter {

    data class LispError(val name: String, val position: LispPosition)


    val errors = listOf<LispError>()

    fun reportError(name: String, position: HasLispPosition) {
        println("LISP ERROR: $name at ${position.position}")
    }


}
