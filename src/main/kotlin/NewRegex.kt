class NewRegex(val rx: String?=null) {
    private var root = Node.new()

    init {
        if (rx != null) {
            parse(rx)
        }
    }

    class SyntaxException(msg: String) : Exception(msg)

    class Group(val start: Node, val capture: Boolean) {
        var repeat = false
        var lazy = false
        val tails = mutableListOf<Node>()
    }

    fun show() = root.getAllNodes().joinToString("\n") { it.show() }

    fun parse(rx: String) {
        val groupStack = mutableListOf<Group>()
        var prevNode: Node = root
        var state = State.none
        var endCaptures = 0
        val choiceChars = mutableListOf<Char>()
        var choiceRange = false
        val choices = mutableListOf<Node>()
        val poppedGroups = mutableListOf<Group>()
        var index = 0
        var lazy = true

        fun throwIf(pred: Boolean, msg: String) {
            if (pred) {
                throw SyntaxException("$msg near '${rx.subSequence(0, index)}'")
            }
        }

        fun newNode() =
            Node.new()
                .also {
                    prevNode = it
                }

        fun pushGroup(n: Node, capture: Boolean) =
            Group(n, capture).also { groupStack.add(it) }

        fun popGroup() =
            (groupStack.lastOrNull()
                ?.also { group ->
                    if (group.tails.isNotEmpty()) {
                        group.tails.add(prevNode)
                        prevNode = Node.new()
                        group.tails.forEach{ n -> n.addTransition(Transition.lambda(prevNode)) }
                    }
                    groupStack.removeLast()
                    poppedGroups.add(group)
                } ?: throwIf(true, "unmatched right parenthesis").let { null })!!

        fun makeRepeat(start: Node, makeTran: (Node)->Transition = { n -> Transition.exitRepeat(n) }) {
            val myStart =
                when {
                    start.repeatStart -> {
                        start.interposeNull()
                        start
                    }
                    start == root ->
                        start.interposeNull()
                    else -> start
                }
            prevNode.addTransition(Transition.repeat(myStart))
            myStart.transitions.forEach{ t -> if (t.consumes) t.setRepeatStart() }
            val n = Node.new()
            myStart.addTransition(makeTran(n))
            myStart.setRepeatStart()
            prevNode = n
        }

        fun makeOptional(startNode: Node) {
            startNode.addTransition(Transition.lambda(prevNode))
        }

        fun flush() {
            when(state) {
                State.lparen ->
                    pushGroup(prevNode, capture=true)
                State.lparenNoCapture ->
                    pushGroup(prevNode, capture=false)
                State.rparen -> {
                    popGroup()
                }
                State.rparenStar -> {
                    with(popGroup()) {
                        makeRepeat(this.start)
                    }
                }
                State.rparenStarLazy -> {
                    with(popGroup()) {
                        this.lazy = true
                        makeRepeat(this.start)
                    }
                }
                State.rparenPlus -> {
                    with(popGroup()) {
                        makeRepeat(this.start) { n -> Transition.counted(n, 1, null) }
                    }
                }
                State.rparenPlusLazy -> {
                    with(popGroup()) {
                        this.lazy = true
                        makeRepeat(this.start) { n -> Transition.counted(n, 1, null) }
                    }
                }
                State.star -> {
                    val prevPrev = prevNode.findPrevious(root).first()
                    makeRepeat(prevPrev)
                }
                State.query -> {
                    val prevPrev = prevNode.findPrevious(root).first()
                    makeOptional(prevPrev)
                }
                State.rparenQ -> {
                    with(popGroup()) {
                        makeOptional(this.start)
                    }
                }
                State.plus -> {
                    val prevPrev = prevNode.findPrevious(root).first()
                    makeRepeat(prevPrev) { n -> Transition.counted(n, 1, null) }
                }
            }
            state = State.none
        }

        rx.forEachIndexed{ idx, ch->
            index = idx
            when {
                state == State.lsquare || state == State.lsquareNot ->
                    when {
                        choiceRange -> {
                            choiceRange = false
                            val startCh = choiceChars.last()
                            choiceChars.dropLast(1)
                            (choiceChars.last()..ch).forEach { choiceChars.add(it) }
                        }
                        ch == '-' && choiceChars.isNotEmpty() -> {
                            choiceRange = true
                        }
                        ch==']' -> {
                            state = State.none
                        }
                        else -> choiceChars.add(ch)
                    }
                ch == '(' -> {
                    flush()
                    state = State.lparen
                }
                ch == '[' -> {
                    flush()
                    state = State.lsquare
                    choices.add(Node.new())
                    prevNode = choices.last()
                }
                ch == ')' -> {
                    flush()
                    state = State.rparen
                }
                ch == '*' && state == State.rparen ->
                    state = State.rparenStar
                ch == '*' -> {
                    flush()
                    state = State.star
                }
                ch == '+' && state == State.rparen ->
                    state = State.rparenPlus
                ch == '+' -> {
                    flush()
                    state = State.plus
                }
                ch == '^' && state == State.lsquare ->
                    state = State.lsquareNot
                ch == '?' && state == State.star ->
                    state = State.lazyStar
                ch == '?' && state == State.plus ->
                    state = State.lazyPlus
                ch == '?' && state == State.lparen ->
                    state = State.lparenQ
                ch == '?' && state == State.rparen ->
                    state = State.rparenQ
                ch == '?' && state == State.rparenStar ->
                    state = State.rparenStarLazy
                ch == '?' && state == State.rparenPlus ->
                    state = State.rparenPlusLazy
                ch == '?' ->
                    state = State.query
                ch == ':' && state == State.lparenQ ->
                    state = State.lparenNoCapture
                ch == '|' -> {
                    flush()
                    if (groupStack.isEmpty()) {
                        pushGroup(root, capture=false)
                    }
                    with (groupStack.last()!!) {
                        tails.add(prevNode)
                        prevNode = start
                    }
                }
                ch == '\\' -> {
                    flush()
                    state = State.escape
                }
                ch == '.' -> {
                    flush()
                    with (prevNode) {
                        addTransition(Transition.dot(newNode()))
                    }
                }
                else -> {
                    flush()
                    with(prevNode) {
                        addTransition(Transition.exact(newNode(), ch))
                    }
                }
            }
        }
        flush()
        if (groupStack.isNotEmpty()) {
            popGroup()
        }
        prevNode.setTerminal()
        root.getAllNodes().forEach{ it.finalize() }
    }

    fun match(str: String, verbose: Boolean = false): Boolean {
        val start = root.makeClosure(listOf(Context(root, root)))
        return str.fold(start) { contexts, ch ->
            (contexts.map { ctx ->
                ctx.eval(ch)
            }.flatten()
                .groupBy { it.node }
                .values
                .map { it.collapse() }
                .flatten())
                .also { if (verbose) println(it) }
        }.any { it.node.terminal }
    }


    companion object {
        enum class State {
            none,
            lparen,
            lparenQ,
            lparenNoCapture,
            lsquare,
            lsquareNot,
            rparen,
            rparenStar,
            rparenStarLazy,
            rparenQ,
            rparenPlus,
            rparenPlusLazy,
            star,
            lazyStar,
            plus,
            lazyPlus,
            query,
            choice,
            escape,
        }
    }
}