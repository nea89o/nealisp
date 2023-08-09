package moe.nea.lisp

sealed class LispAst : HasLispPosition {


    abstract fun toSource(): String


    data class Program(override val position: LispPosition, val nodes: List<LispNode>) : LispAst() {
        override fun toSource(): String {
            return nodes.joinToString("\n") {
                it.toSource()
            }
        }
    }

    sealed class LispNode : LispAst()
    data class Atom(override val position: LispPosition, val label: String) : LispNode() {
        override fun toSource(): String {
            return ":$label"
        }
    }

    data class Reference(override val position: LispPosition, val label: String) : LispNode() {
        override fun toSource(): String {
            return label
        }
    }

    data class Parenthesis(override val position: LispPosition, val items: List<LispNode>) : LispNode() {
        override fun toSource(): String {
            return items.joinToString(" ", "(", ")") { it.toSource() }
        }
    }

    data class StringLiteral(override val position: LispPosition, val parsedString: String) : LispNode() {
        override fun toSource(): String {
            return "\"${parsedString.replace("\\", "\\\\").replace("\"", "\\\"")}\"" // TODO: better escaping
        }
    }
}
