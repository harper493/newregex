class Node (val id: Int) {
    private val captures = mutableListOf<Capture>()
    val transitions = mutableListOf<Transition>()
    private val nullTransitions = mutableSetOf<Node>();
    var terminal = false; private set

    fun clone() =
        let {
            val n = new()
            n.copyTransitions(this)
            n
        }

    private fun copyTransitions(other: Node) {
        transitions.addAll(other.transitions.map{ it.clone() })
    }

    override fun toString() = "$id"

    fun show() =
        "$id ${ if (terminal) "T" else ""}" +
                transitions.map{ "\n   $it" }.joinToString("") +
                if (nullTransitions.isNotEmpty()) "\n   +null -> $nullTransitions" else ""

    fun setCapture(c: Capture) =
        also {
            captures.add(c)
        }

    fun setTerminal() =
        also {
            terminal = true
        }

    fun addTransition(t: Transition) =
        also {
            transitions.add(t)
        }

    fun addNullTransition(n: Node) =
        also {
            nullTransitions.add(n)
        }

    fun withNulls() =
        setOf(this) + nullTransitions

    fun setStar(lazy: Boolean) =
        also {

        }

    fun interposeNull() =
        let {
            val newNull = clone()
            transitions.clear()
            addTransition(Transition.nullTransition(newNull))
            newNull
        }

    fun eval(ch: Char) =
        transitions
            .mapNotNull {it(ch) }
            .map { it.withNulls() }
            .flatten()

    fun makeNullTransitions(done: MutableSet<Node>) {
        if (this !in done) {
            done.add(this)
            transitions.forEach { it.next.makeNullTransitions(done) }
            nullTransitions.addAll(transitions.filter{ it.isNull }.map{ it.next })
            nullTransitions.addAll(transitions.filter{ it.isNull }.map{ it.next.nullTransitions }.flatten() )
        }
    }

    fun getAllNodes() =
        let {
            val done = mutableSetOf<Node>()
            successors(done)
        }

    private fun successors(done: MutableSet<Node>): List<Node> =
        let {
            done.add(this)
            listOf(this) +
                    transitions
                        .filter { it.next !in done }
                        .map { it.next.successors(done) }
                        .flatten()
        }

    companion object {
        var nodeId = 1
        fun new() =
            Node(nodeId)
                .also { ++nodeId }
    }
}