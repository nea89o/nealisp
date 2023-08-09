package moe.nea.lisp

data class LispParsingError(val baseString: String, val offset: Int, val mes0: String) :
    Exception("$mes0 at $offset in `$baseString`.")