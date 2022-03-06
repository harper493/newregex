class Transition(
    var next: Node,
    val descr: String,
    val consumes: Boolean = true,
    val alwaysNull: Boolean = false,
    val maxRepeats: Int? = null,
    val action: (Context)->Unit = { },
    val eval: (Char, Context)->Boolean ) {

    var repeat = false; private set
    var repeatStart = false; private set

    fun clone() =
        Transition(next, descr, consumes, alwaysNull, maxRepeats, action, eval)

    fun matches(index: Int, ch: Char, ctx: Context) =
        if (consumes && eval(ch, ctx)) {
            advance(index, ctx)
        } else null

    fun lambda(index: Int, ctx: Context) =
        if (alwaysNull || (!consumes && eval('\u0000', ctx))) {
            advance(index, ctx)
        } else null

    private fun advance(index: Int, ctx: Context): Context {
        val newctx = ctx.clone(this, index)
        action(newctx)
        if (next.repeatStart && !repeat) {
            newctx.pushRepeat()
        }
        return newctx
    }

    fun modifyNext(from: Node,to_: Node) =
        also {
            if (next==from) next = to_
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
        fun exact(n: Node, minChar: Char, maxChar: Char? = null) =
            Transition(n, "exact '$minChar'"){ ch, ctx -> ch>=minChar && ch <= (maxChar ?: minChar) }

        fun dot(n: Node) =
            Transition(n, "dot"){ ch, ctx -> true }

        fun lambda(n: Node) =
            Transition(n, "null", false, true){ ch, ctx -> true }

        fun exitRepeat(n: Node) =
            Transition(n, "exitRepeat", false, true,
                action = { ctx -> ctx.popRepeat() }){ ch, ctx -> true }

        fun repeat(n: Node) =
            Transition(n, "repeat", false,
            action = { ctx -> ctx.countRepeat() }) { ch, ctx -> true }
                .setRepeat(true)

        fun counted(n: Node, minRepeats: Int, maxRepeats: Int?) =
            Transition(n, "counted($minRepeats..$maxRepeats)", false, maxRepeats = maxRepeats,
                action = { ctx -> ctx.popRepeat() })
            { ch, ctx -> ctx.repeats in minRepeats..(maxRepeats ?: ctx.repeats) }
    }
}