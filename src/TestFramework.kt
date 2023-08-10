package moe.nea.lisp

import java.time.Instant

object TestFramework {
    data class TestFailure(
        val callsite: LispAst,
        val message: String,
    )

    data class TestResult(
        val name: String,
        val failures: List<TestFailure>,
        val wasSkipped: Boolean,
    )

    data class TestSuite(
        val name: String,
        val startTime: Instant,
        var isTesting: Boolean,
        val allTests: MutableList<TestResult>,
        val testList: List<String>,
        val isWhitelist: Boolean,
    )

    data class ActiveTest(
        val testName: String,
        val currentFailures: MutableList<TestFailure>,
        var canMultifail: Boolean,
        val suite: TestSuite,
    )

    object TestSuiteMeta : StackFrame.MetaKey<TestSuite>
    object ActiveTestMeta : StackFrame.MetaKey<ActiveTest>

    val testBinding = LispData.externalRawCall("ntest.test") { context, callsite, stackFrame, args ->
        runTest(context, callsite, stackFrame, args)
        return@externalRawCall LispData.LispNil
    }

    val failTestBinding = LispData.externalCall("ntest.fail") { args, reportError ->
        val message = CoreBindings.stringify(args.singleOrNull() ?: return@externalCall reportError("Needs a message"))
        LispData.externalRawCall("ntest.fail.r") { context, callsite, stackFrame, args ->
            val activeTest = stackFrame.getMeta(ActiveTestMeta)
                ?: return@externalRawCall context.reportError("No active test", callsite)
            activeTest.currentFailures.add(TestFailure(callsite, message))
            return@externalRawCall LispData.LispNil
        }
    }

    val realizedTestModule = mapOf(
        "ntest.test" to testBinding,
        "ntest.fail" to failTestBinding,
    )

    fun runTest(
        context: LispExecutionContext,
        callsite: LispAst,
        stackFrame: StackFrame,
        args: List<LispAst.LispNode>
    ) {
        val meta = stackFrame.getMeta(TestSuiteMeta) ?: return
        if (!meta.isTesting) return
        if (args.size != 2) {
            context.reportError("Test case needs to be defined by a name and an executable", callsite)
            return
        }
        val (name, prog) = args
        val testName = when (val n = context.resolveValue(stackFrame, name)) {
            is LispData.Atom -> n.label
            is LispData.LispString -> n.string
            else -> {
                context.reportError("Test case needs an atom or string as name", name)
                return
            }
        }
        if (testName in meta.testList != meta.isWhitelist) {
            meta.allTests.add(TestResult(testName, listOf(), true))
            return
        }
        val child = stackFrame.fork()
        val test = ActiveTest(testName, mutableListOf(), false, meta)
        child.setMeta(ActiveTestMeta, test)
        context.resolveValue(child, prog)
        meta.allTests.add(TestResult(test.testName, test.currentFailures, false))
    }

    fun setup(stackFrame: StackFrame, name: String, testList: List<String>, isWhitelist: Boolean): TestSuite {
        val ts = TestSuite(name, Instant.now(), true, mutableListOf(), testList, isWhitelist)
        stackFrame.setMeta(TestSuiteMeta, ts)
        return ts
    }
}
