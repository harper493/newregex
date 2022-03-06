class Node (val id: Int) {
    val transitions = mutableListOf<Transition>()
    private val predecessors = mutableSetOf<Node>()
    private val nullTransitions = mutableSetOf<Node>()
    var terminal = false; private set
    var repeatStart = false; private set
    var transient: Boolean = false; private set
    var maxRepeats: Int? = null; private set
    val groupStarts = mutableListOf<NewRegex.Group>()
    val captureStarts = mutableListOf<NewRegex.Group>()
    var groupEnds = mutableListOf<NewRegex.Group>()

    fun clone() =
        let {
            val n = new()
            n.copyTransitions(this)
            n.repeatStart = repeatStart
            n.terminal = terminal
            n
        }

    private fun copyTransitions(other: Node) {
        transitions.addAll(other.transitions.map{ it.clone() })
    }

    override fun toString() = "$id"

    fun show() =
        "$id ${ if (terminal) "T" else ""}${ if (repeatStart) "R" else ""}${ if (transient) "X" else ""}" +
                (groupStarts.describe("G")) +
                (groupEnds.describe("E")) +
                (captureStarts.describe("C")) +
                transitions.joinToString("") { "\n   $it" } +
                if (nullTransitions.isNotEmpty()) "\n   +null -> $nullTransitions" else ""

    fun finalize() {
        if (transitions.all{ !it.consumes } && !terminal) {
            transient = true
        }
        maxRepeats = transitions.filter{ it.maxRepeats != null}.maxOfOrNull{ it.maxRepeats!! }
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

    fun addGroup(g: NewRegex.Group) =
        also {
            groupStarts.add(g)
        }

    fun addGroupEnd(g: NewRegex.Group) =
        also {
            groupEnds.add(g)
        }

    fun addCaptureStart(g: NewRegex.Group) =
        also {
            captureStarts.add(g)
        }

    fun withNulls() =
        setOf(this) + nullTransitions

    fun setStar(lazy: Boolean) =
        also {

        }

    fun interposeNull() =
        let {
            val newNode = clone()
            transitions.clear()
            addTransition(Transition.lambda(newNode))
            getAllNodes().forEach{ node -> node.transitions.forEach{ t -> t.modifyNext(this, newNode)}}
            newNode
        }

    fun findPrevious(root: Node) =
        root.getAllNodes()
            .filter{ it.transitions.any{ it.next==this } }

    fun eval(index: Int, ch: Char, ctx: Context) =
            makeClosure(index, transitions.mapNotNull { it.matches(index, ch, ctx) })
                .filter{ !it.node.transient }

    fun makeClosure(index: Int, contexts: List<Context>) =
        iterativeClosure(contexts) { c2 -> c2.node.transitions.mapNotNull { t -> t.lambda(index, c2) } }

    fun getAllNodes() =
        let {
            val done = mutableSetOf<Node>()
            successors(done)
        }

    fun getNextNodes() =
        transitions.map{ it.next }.toSet()

    private fun successors(done: MutableSet<Node>): Set<Node> =
        let {
            done.add(this)
            setOf(this) +
                    transitions
                        .asSequence()
                        .filter { it.next !in done }
                        .map { it.next.successors(done) }
                        .flatten()
                        .toSet()
                        .sortedBy{ it.id }
                        .toList()
        }

    companion object {
        private var nodeId = 1
        fun reset() {
            nodeId = 1
        }
        fun new() =
            Node(nodeId)
                .also { ++nodeId }
    }
}