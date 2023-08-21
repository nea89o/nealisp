import moe.nea.lisp.*
import moe.nea.lisp.bind.AutoBinder
import moe.nea.lisp.bind.LispBinding
import java.io.File
import javax.xml.stream.XMLOutputFactory
import kotlin.system.exitProcess

object T

object TestBindings {
    @LispBinding("funny-method")
    fun funnyMethod(arg: Int, test: String, boolean: Boolean, vararg ast: LispAst): LispData {
        if (boolean)
            println("From java: $test")
        ast.forEach { println(it.toSource()) }
        return LispData.LispNumber(arg.toDouble())
    }

}

fun main() {
    val otherP = LispParser.parse(File(T::class.java.getResource("/scratch.lisp")!!.file))
    val executionContext = LispExecutionContext()
    executionContext.setupStandardBindings()
    executionContext.registerModule(
        "secondary",
        LispParser.parse(File(T::class.java.getResource("/secondary.lisp")!!.file))
    )
    val bindings = executionContext.genBindings()
    AutoBinder().bindTo(TestBindings, bindings)
    val testResults = executionContext.runTests(otherP, "Test", bindings)
    val w = XMLOutputFactory.newFactory()
        .createXMLStreamWriter(File("TestOutput.xml").bufferedWriter())
    TestResultFormatter.write(w, listOf(testResults))
    w.close()
    if (testResults.allTests.any { it.failures.isNotEmpty() }) {
        exitProcess(1)
    }
}
