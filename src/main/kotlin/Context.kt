class Context(val node: Node) {

    class Counter() {
        var count = 0; private set

        override fun toString() =
            "$count"

        fun inc() {
            ++count
        }
    }

    var repeatStack = mutableListOf<Counter>()

    val repeats: Int get() = repeatStack.lastOrNull()?.count ?: 0

    override fun toString() =
        "$node" + if (repeats>0) "($repeats)" else ""

    fun clone(n: Node) =
        Context(n)
            .also { it.repeatStack = this.repeatStack.toMutableList() }

    fun countRepeat(doCount: Boolean = true) =
        also {
            if (doCount) repeatStack.lastOrNull()?.inc()
        }

    fun manageRepeat(prev: Node, transition: Transition) =
        also{
            if (transition.repeat) {
                countRepeat(true)
            } else {
                if (prev.repeatStart && !transition.repeatStart) {
                    popRepeat()
                }
                if (node.repeatStart) {
                    pushRepeat()
                }
            }

        }

    fun pushRepeat() =
        also {
            repeatStack.add(Counter())
        }

    fun popRepeat() =
        also {
            repeatStack.removeLast()
        }

    fun eval(ch: Char) =
        node.eval(ch, this)

    fun collapseWith(other: Context): Context? =
        if (repeats==other.repeats && node==other.node) this else null

}

fun List<Context>.collapseOne() =
    fold(listOf<Context>()) { acc, ctx ->
        let {
            val trial = acc.firstOrNull()?.collapseWith(ctx) ?: ctx
            if (trial==null) {
                listOf(listOf(acc.firstOrNull() ?: ctx), acc.drop(1), listOf(ctx))
            } else {
                listOf(listOf(trial), acc.drop(1))
            }
        }.flatten()
    }

fun List<Context>.collapse(): List<Context> =
    if (isEmpty()) {
        listOf<Context>()
    } else {
        with(collapseOne()) {
            (listOf(first()) + drop(1).collapse())
        }
    }

fun List<Context>.toString() =
    "[ ${map{ "$it" }.joinToString(", ")} ]"

