class Node (val id: Int) {
    private val captures = mutableListOf<Capture>()
    val transitions = mutableListOf<Transition>()
    val predecessors = mutableSetOf<Node>()
    private val nullTransitions = mutableSetOf<Node>();
    var terminal = false; private set
    var repeatStart = false; private set

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
        "$id ${ if (terminal) "T" else ""}${ if (repeatStart) "R" else ""}" +
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

    fun setRepeatStart(rs: Boolean = true) =
        also {
            repeatStart = rs
        }

    fun addTransition(t: Transition) =
        also {
            transitions.add(t)
        }

    fun addPredecessor(n: Node) =
        also {
            predecessors.add(n)
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
            addTransition(Transition.lambda(newNull))
            newNull
        }

    fun findPrevious(root: Node) =
        root.getAllNodes()
            .filter{ it.transitions.any{ it.next==this } }

    fun eval(ch: Char, ctx: Context) =
        transitions
            .mapNotNull { it.matches(ch, ctx) }
            .let { cl ->
                    iterativeClosure(cl) { c2 -> c2.node.transitions.mapNotNull { t -> t.lambda(c2) } }
            }

    fun getAllNodes() =
        let {
            val done = mutableSetOf<Node>()
            successors(done)
        }

    private fun successors(done: MutableSet<Node>): Set<Node> =
        let {
            done.add(this)
            setOf(this) +
                    transitions
                        .filter { it.next !in done }
                        .map { it.next.successors(done) }
                        .flatten()
                        .toSet()
                        .sortedBy{ it.id }
        }

    companion object {
        var nodeId = 1
        fun new() =
            Node(nodeId)
                .also { ++nodeId }
    }
}