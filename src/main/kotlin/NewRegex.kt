class NewRegex(val rx: String?=null) {
    private var root = Node(0)

    init {
        if (rx != null) {
            parse(rx)
        }
    }

    class SyntaxException(msg: String) : Exception(msg)

    class Group(val start: Node) {
        var repeat = false
        var lazy = false
        var capture = true
    }

    fun show() = root.getAllNodes().map{ it.show() }.joinToString("\n")

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

        fun pushGroup() =
            Group(prevNode).also { groupStack.add(it) }

        fun popGroup() =
            groupStack.lastOrNull()
                .also {
                    throwIf(groupStack.isEmpty(), "unmatched right parenthesis")
                    groupStack.removeLast()
                    poppedGroups.add(it!!)
                }!!

        fun makeRepeat(startNode: Node) {
            val n = startNode.interposeNull()
            prevNode.addTransition(Transition.nullTransition(n))
            prevNode = n
        }

        fun makeOptional(startNode: Node) {
            startNode.addTransition(Transition.nullTransition(prevNode))
        }

        fun flush() {
            when(state) {
                State.lparen ->
                    pushGroup()
                State.lparenNoCapture ->
                    pushGroup()
                        .also{ it.capture = false }
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
                ch == '?' ->
                    state = State.query
                ch == ':' && state == State.lparenQ ->
                    state = State.lparenNoCapture
                ch == '|' -> {
                    flush()
                    choices.add(Node.new())
                    prevNode = choices.last()
                    state = State.none
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
                        addTransition(Transition.exactMatch(newNode(), ch))
                    }
                }
            }
        }
        flush()
        prevNode.setTerminal()
        root.makeNullTransitions(mutableSetOf<Node>())
    }

    fun match(str: String, verbose: Boolean = false) =
        let {
            var transitions = listOf(root)
            str.forEach { ch ->
                if (verbose) println("$ch: ${  transitions.map{ "$it" }.joinToString(", ")}")
                transitions = transitions.map{ it.eval(ch)}.flatten().filterNotNull()
                if (verbose) println(transitions.map{ it.show() }.joinToString("\n"))
            }
            transitions.any{ it.terminal }
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