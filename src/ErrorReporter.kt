package moe.nea.lisp

interface ErrorReporter {
    fun reportError(string: String): LispData
    fun reportError(string: String, exception: Throwable): LispData {
        return reportError("$string: $exception")
    }
}