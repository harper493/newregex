class Context(val node: Node, private val prev: Node) {

    class Counter(init: Int =  0) {
        var count = init; private set

        override fun toString() =
            "$count"

        fun inc() {
            ++count
        }
    }

    private var repeatStack = mutableListOf<Counter>()

    val repeats: Int get() = repeatStack.lastOrNull()?.count ?: 0

    override fun toString() =
        "$prev\u2192$node" + if (repeatStack.isNotEmpty()) "(${repeatStack.joinToString(",") { "$it" }})" else ""

    fun clone(n: Node) =
        Context(n, node)
            .also { newctx -> newctx.repeatStack = this.repeatStack.map { ctr -> Counter(ctr.count)}.toMutableList() }

    fun countRepeat(doCount: Boolean = true) =
        also {
            if (doCount) repeatStack.lastOrNull()?.inc()
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
        if (node==other.node &&
            (node.maxRepeats==null || repeats==other.repeats || minOf(repeats, other.repeats) >= node.maxRepeats!!)) this else null
            .also { val i = 1}

}

fun List<Context>.collapseOne() =
    fold(listOf<Context>()) { acc, ctx ->
        if (acc.isEmpty()) {
            listOf(ctx)
        } else {
            let {
                val trial = acc.firstOrNull()?.collapseWith(ctx)
                if (trial == null) {
                    listOf(listOf(acc.firstOrNull() ?: ctx), acc.drop(1), listOf(ctx))
                } else {
                    listOf(listOf(trial), acc.drop(1))
                }
            }.flatten()
        }
    }

fun List<Context>.collapse(): List<Context> =
    if (isEmpty()) {
        listOf()
    } else {
        with(collapseOne()) {
            (listOf(first()) + drop(1).collapse())
        }
    }

fun List<Context>.toString() =
    "[ ${joinToString(", ") { "$it" }} ]"

