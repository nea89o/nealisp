package moe.nea.lisp

data class LispPosition(
    val start: Int,
    val end: Int,
    val fileName: String,
    val fileContent: String,
) : HasLispPosition {
    val startLine by lazy { fileContent.substring(0, start).count { it == '\n' } + 1 }
    val startColumn by lazy { start - fileContent.substring(0, start).indexOfLast { it == '\n' } }
    val endLine by lazy { fileContent.substring(0, end).count { it == '\n' } + 1 }
    val endColumn by lazy { end - fileContent.substring(0, end).indexOfLast { it == '\n' } }
    override val position get() = this
    override fun toString(): String {
        return "at $fileName:$startLine:$startColumn until $endLine:$endColumn"
    }
}

interface HasLispPosition {
    val position: LispPosition
}
