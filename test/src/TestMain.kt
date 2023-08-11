import moe.nea.lisp.LispExecutionContext
import moe.nea.lisp.LispParser
import moe.nea.lisp.TestResultFormatter
import java.io.File
import javax.xml.stream.XMLOutputFactory

object TestMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val reportFile = System.getProperty("test.report")
        val modulesToTest = System.getProperty("test.suites").split(":")
        val modulePath = System.getProperty("test.imports").split(":")
        val executionContext = LispExecutionContext()
        executionContext.setupStandardBindings()
        modulePath.forEach {
            executionContext.registerModule(it, LispParser.parse(File(T::class.java.getResource("/$it.lisp")!!.file)))
        }
        val allResults = modulesToTest.map {
            executionContext.runTests(
                LispParser.parse(File(T::class.java.getResource("/$it.lisp")!!.file)),
                it,
            )
        }
        val w = XMLOutputFactory.newFactory()
            .createXMLStreamWriter(File(reportFile).bufferedWriter())
        TestResultFormatter.write(w, allResults)
        w.close()

    }
}