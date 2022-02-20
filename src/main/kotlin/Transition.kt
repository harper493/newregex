class Transition(var next: Node, val descr: String, val isNull: Boolean = false, val fn: (Char)->Boolean) {

    fun clone() =
        Transition(next, descr, isNull, fn)

    operator fun invoke(ch: Char) =
        if (fn(ch)) next else null

    fun setNext(n: Node) =
        also {
            next = n
        }

    override fun toString() =
        "$descr -> $next"

    companion object {
        fun exactMatch(n: Node, minChar: Char, maxChar: Char? = null) =
            Transition(n, "exact '$minChar'"){ ch -> ch>=minChar && ch <= (maxChar ?: minChar) }

        fun dot(n: Node) =
            Transition(n, "dot"){ true }

        fun nullTransition(n: Node) =
            Transition(n, "null", true){ false }
    }
}