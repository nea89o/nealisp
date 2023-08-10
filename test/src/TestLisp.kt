import moe.nea.lisp.LispExecutionContext
import moe.nea.lisp.LispParser
import java.io.File

object T

fun main() {
    val otherP = LispParser.parse(File(T::class.java.getResource("/test.lisp")!!.file))
    val executionContext = LispExecutionContext()
    executionContext.setupStandardBindings()
    executionContext.registerModule("secondary", LispParser.parse(File(T::class.java.getResource("/secondary.lisp")!!.file)))
    val bindings = executionContext.genBindings()
    executionContext.executeProgram(bindings, otherP)
}
