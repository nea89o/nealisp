import moe.nea.lisp.CoreBindings
import moe.nea.lisp.LispExecutionContext
import moe.nea.lisp.LispParser
import java.io.File

object T

fun main() {
    val otherP = LispParser.parse(File(T::class.java.getResource("/test.lisp")!!.file))
    val executionContext = LispExecutionContext()
    val bindings = executionContext.genBindings()
    CoreBindings.offerAllTo(bindings)
    executionContext.executeProgram(bindings, otherP)
}
