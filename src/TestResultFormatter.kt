package moe.nea.lisp

import java.text.SimpleDateFormat
import java.util.*
import javax.xml.stream.XMLStreamWriter

class TestResultFormatter(private val writer: XMLStreamWriter) {
    companion object {
        private val timestampFormatter = SimpleDateFormat(
            "yyyy-MM-dd'T'hh:mm:ss"
        )

        fun write(writer: XMLStreamWriter, testResults: List<TestFramework.TestSuite>) {
            TestResultFormatter(writer).writeAll(testResults)
        }
    }

    fun writeTestSuite(testSuite: TestFramework.TestSuite) {
        writer.writeStartElement("testsuite")
        writer.writeAttribute("name", testSuite.name)
        writer.writeAttribute("tests", testSuite.allTests.size.toString())
        writer.writeAttribute("skipped", testSuite.allTests.count { it.wasSkipped }.toString())
        writer.writeAttribute("failures", testSuite.allTests.count { it.failures.isNotEmpty() }.toString())
        writer.writeAttribute("errors", "0") // TODO: figure out how to differentiate errors and failures
        writer.writeAttribute("timestamp", timestampFormatter.format(Date.from(testSuite.startTime)))

        writer.writeStartElement("properties")
        writer.writeEndElement()

        testSuite.allTests.forEach {
            writeTestCase(it)
        }

        writer.writeEndElement()
    }

    fun writeTestCase(test: TestFramework.TestResult) {
        writer.writeStartElement("testcase")
        writer.writeAttribute("name", test.name)
        writer.writeAttribute("time", "0.0") // TODO: proper timestamping

        if (test.wasSkipped) {
            writeSkipped()
        }
        for (fail in test.failures) {
            writeFailure(fail)
        }

        writer.writeEndElement()
    }

    fun writeSkipped() {
        writer.writeStartElement("skipped")
        writer.writeEndElement()
    }

    fun writeFailure(fail: TestFramework.TestFailure) {
        writer.writeStartElement("failure")
        writer.writeAttribute("message", fail.message)
        writer.writeCData(fail.callsite.toSource())
        writer.writeEndElement()
    }

    fun writeAll(testResults: List<TestFramework.TestSuite>) {
        writer.writeStartDocument()
        writer.writeStartElement("testsuites")

        testResults.forEach {
            writeTestSuite(it)
        }

        writer.writeEndElement()
        writer.writeEndDocument()
    }
}