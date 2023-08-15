import moe.nea.lisp.LispExecutionContext
import moe.nea.lisp.LispParser
import moe.nea.lisp.TestResultFormatter
import java.io.File
import javax.xml.stream.XMLOutputFactory
import kotlin.system.exitProcess

object T


fun main() {
    val otherP = LispParser.parse(File(T::class.java.getResource("/test.lisp")!!.file))
    val executionContext = LispExecutionContext()
    executionContext.setupStandardBindings()
    executionContext.registerModule(
        "secondary",
        LispParser.parse(File(T::class.java.getResource("/secondary.lisp")!!.file))
    )
    val bindings = executionContext.genBindings()
    val testResults = executionContext.runTests(otherP, "Test", bindings)
    val w = XMLOutputFactory.newFactory()
        .createXMLStreamWriter(File("TestOutput.xml").bufferedWriter())
    TestResultFormatter.write(w, listOf(testResults))
    w.close()
    if (testResults.allTests.any { it.failures.isNotEmpty() }) {
        exitProcess(1)
    }
}
