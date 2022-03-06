class Context(val node: Node,
              index: Int,
              private val prev: Node,
              private val repeatStack: MutableList<Counter> = mutableListOf(),
              val captures: MutableList<Capture> = mutableListOf(),
              trans: Transition? = null) {
    var lazy: Boolean = false

    init {
        node.groupEnds.forEach { g ->
            captures.find{ it.group==g }?.let { it.terminate(index+1) }
        }
        node.captureStarts.forEach{ g ->
            captures.find{ it.group==g && !(trans?.repeat ?: false)}
                ?.let { it.restart(index+1) }
                ?: captures.add(Capture(g, index+1)) }
    }


    class Counter(init: Int =  0) {
        var count = init; private set

        override fun toString() =
            "$count"

        fun inc() {
            ++count
        }

        fun clone() =
            Counter(count)
    }

    class Capture(val group: NewRegex.Group, var start: Int, var end: Int? = null) {
        var goodStart: Int? = null

        override fun toString() =
            "G${group.id}:" + ((goodStart?.let{"$it..$end/$start"}) ?: "$start")

        fun merge(other: Capture, ctx: Context) =
            if (group==other.group) {
                Capture(group, if (ctx.lazy) maxOf(start, other.start) else minOf(start, other.start))
            } else {
                null
            }

        fun terminate(index: Int) =
            also {
                if (index > start && start!=goodStart) {
                    goodStart = start
                    end = index
                }
            }

        fun restart(index: Int) =
            also {
                goodStart = start
                start = index
            }
        
        fun clone() =
            Capture(group, start, end).also{ it.goodStart = goodStart }

        fun get(str: String): String? =
            if (goodStart!=null) str.subSequence(goodStart!!, end!!).toString() else null
    }


    val repeats: Int get() = repeatStack.lastOrNull()?.count ?: 0

    override fun toString() =
        "$prev\u2192$node" +
                if (repeatStack.isNotEmpty()) "(${repeatStack.joinToString(",") { "$it" }})" else "" +
                    if (captures.isNotEmpty()) "[${captures.map{"$it"}.joinToString(",")}]" else ""

    fun clone(trans: Transition, index: Int) =
        Context(trans.next, index, node,
            repeatStack.map { it.clone() }.toMutableList(),
            captures.map { it.clone() }.toMutableList(),
            trans)

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

    fun findCapture(g: NewRegex.Group) =
        captures.find{ cap -> cap.group==g }

    fun eval(index: Int, ch: Char) =
        node.eval(index, ch, this)

    fun collapseWith(other: Context): Context? =
        if (node==other.node &&
            (node.maxRepeats==null || repeats==other.repeats || minOf(repeats, other.repeats) >= node.maxRepeats!!)) {
                captures.fold<Capture, Context?>(null) { r, cap ->
                    r ?: other.findCapture(cap.group)
                        ?.let{ otherCap ->
                            when {
                                !cap.group.repeat || cap.goodStart==null || otherCap.goodStart==null -> null
                                cap.group.lazy && cap.goodStart!! < otherCap.goodStart!! -> this
                                cap.group.lazy && cap.goodStart!! > otherCap.goodStart!! -> other
                                !cap.group.lazy && cap.goodStart!! < otherCap.goodStart!! -> other
                                !cap.group.lazy && cap.goodStart!! > otherCap.goodStart!! -> this
                                else -> null
                            }
                        } ?: this
                }
        } else null
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

fun List<Context>.collapseOneNode(): List<Context> =
    if (isEmpty()) {
        listOf()
    } else {
        with(collapseOne()) {
            (listOf(first()) + drop(1).collapse())
        }
    }

fun List<Context>.collapse(): List<Context> =
    groupBy { it.node }
        .values
        .map { it.collapseOneNode() }
        .flatten()

fun List<Context>.toString() =
    "[ ${joinToString(", ") { "$it" }} ]"

