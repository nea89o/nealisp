package moe.nea.lisp

import java.util.*

class StringRacer(val filename: String, val backing: String) {
    var idx = 0
    val stack = Stack<Int>()

    fun pushState() {
        stack.push(idx)
    }

    fun popState() {
        idx = stack.pop()
    }

    fun span(start: Int) = LispPosition(start, idx, filename, backing)

    fun discardState() {
        stack.pop()
    }

    fun peek(count: Int): String {
        return backing.substring(minOf(idx, backing.length), minOf(idx + count, backing.length))
    }

    fun finished(): Boolean {
        return peek(1).isEmpty()
    }

    fun peekReq(count: Int): String? {
        val p = peek(count)
        if (p.length != count)
            return null
        return p
    }

    fun consumeCountReq(count: Int): String? {
        val p = peekReq(count)
        if (p != null)
            idx += count
        return p
    }

    fun tryConsume(string: String): Boolean {
        val p = peek(string.length)
        if (p != string)
            return false
        idx += p.length
        return true
    }

    fun consumeWhile(shouldConsumeThisString: (String) -> Boolean): String {
        var lastString: String = ""
        while (true) {
            val nextPart = peek(1)
            if (nextPart.isEmpty()) break
            val nextString = lastString + nextPart
            if (!shouldConsumeThisString(nextString)) {
                break
            }
            idx++
            lastString = nextString
        }
        return lastString
    }

    fun expect(search: String, errorMessage: String) {
        if (!tryConsume(search))
            error(errorMessage)
    }

    fun error(errorMessage: String): Nothing {
        throw LispParsingError(backing, idx, errorMessage)
    }

    fun skipWhitespace() {
        consumeWhile { Character.isWhitespace(it.last()) }
    }
}

