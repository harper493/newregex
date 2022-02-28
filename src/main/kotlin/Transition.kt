class Transition(
    var next: Node,
    val descr: String,
    val consumes: Boolean = true,
    val alwaysNull: Boolean = false,
    val eval: (Char, Context)->Boolean ) {

    var repeat = false; private set
    var repeatStart = false; private set

    fun clone() =
        Transition(next, descr, consumes, alwaysNull, eval)

    fun matches(ch: Char, ctx: Context) =
        if (consumes && eval(ch, ctx)) {
            ctx.clone(next)
                .manageRepeat(ctx.node, this)
        } else null

    fun lambda(ctx: Context) =
        if (alwaysNull || (!consumes && eval('\u0000', ctx)?: true)) {
            ctx.clone(next)
                .manageRepeat(ctx.node, this)
        } else null

    fun setNext(n: Node) =
        also {
            next = n
        }

    fun setRepeat(r: Boolean) =
        also {
            repeat = r
        }

    fun setRepeatStart(rs: Boolean = true) =
        also {
            repeatStart = rs
        }

    override fun toString() =
        "$descr -> $next ${ if (repeat) "R" else ""}${ if (repeatStart) "S" else "" }"

    companion object {
        fun exactMatch(n: Node, minChar: Char, maxChar: Char? = null) =
            Transition(n, "exact '$minChar'"){ ch, ctx -> ch>=minChar && ch <= (maxChar ?: minChar) }

        fun dot(n: Node) =
            Transition(n, "dot"){ ch, ctx -> true }

        fun lambda(n: Node) =
            Transition(n, "null", false, true){ ch, ctx -> true }

        fun repeat(n: Node) =
            Transition(n, "repeat", false) { ch, ctx -> true }
                .setRepeat(true)

        fun counted(n: Node, minRepeats: Int, maxRepeats: Int?) =
            Transition(n, "counted($minRepeats..$maxRepeats)", false)
            { ch, ctx -> ctx.repeats in minRepeats..(maxRepeats ?: ctx.repeats) }
    }
}