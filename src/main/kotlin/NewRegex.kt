class NewRegex(val rx: String?=null) {
    private var root = Node.new()
    private val groups = mutableListOf<Group>()

    init {
        if (rx != null) {
            parse(rx)
        }
    }

    class SyntaxException(msg: String) : Exception(msg)

    class Group(val start: Node, val capture: Boolean) {
        val id = groupId.also { ++groupId }
        var lazy = false
        var synthetic = false
        val tails = mutableListOf<Node>()

        fun get(str: String, ctx: Context) =
            ctx.captures
                .find{ it.group==this }
                ?.get(str)
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

        fun newGroup(start: Node, capture: Boolean) =
            Group(start, capture)
                .also { g ->
                    groups.add(g)
                    start.addGroup(g)
                    groupStack.add(g)
                }

        fun pushGroup(capture: Boolean) =
            let {
                val start = prevNode
                start.addTransition(Transition.lambda(newNode()))
                newGroup(start, capture)
            }

        fun getLastGroup() =
            (groupStack.lastOrNull()
                ?: throwIf(true, "unmatched right parenthesis").let { null })!!

        fun endChoice() =
            with (getLastGroup())
            {
                if (tails.isNotEmpty()) {
                    tails.add(prevNode)
                    prevNode = Node.new()
                    tails.forEach { n -> n.addTransition(Transition.lambda(prevNode)) }
                }
            }


        fun popGroup() =
            getLastGroup()
                ?.also { group ->
                    if (group.tails.isNotEmpty()) {
                        group.tails.add(prevNode)
                        prevNode = Node.new()
                        group.tails.forEach { n -> n.addTransition(Transition.lambda(prevNode)) }
                    }
                    prevNode.addGroupEnd(group)
                    groupStack.removeLast()
                }

        fun makeRepeat(lazy: Boolean, fromTail: Boolean = false, makeTran: (Node)->Transition = { n -> Transition.exitRepeat(n) }) {
            endChoice()
            val g = getLastGroup()
            val start = g.start
            val oldPrev = prevNode
            oldPrev.addTransition(Transition.repeat(start))
            start.transitions.forEach{ t -> t.next.addCaptureStart(g) }
            val n = Node.new()
            if (fromTail) {
                oldPrev.addTransition(makeTran(n))
            } else {
                start.addTransition(makeTran(n))
            }
            start.setRepeatStart()
            prevNode = n
            popGroup()
        }

        fun makeOptional() {
            popGroup().start.addTransition(Transition.lambda(prevNode))
        }

        fun makeGroupFromAtom() =
            let {
                val prevPrev = prevNode.findPrevious(root).first()
                prevPrev.interposeNull()
                newGroup(prevPrev, capture = false)
            }

        fun flush() {
            when(state) {
                State.lparen -> {
                    pushGroup(capture=true)
                }
                State.lparenNoCapture -> {
                    pushGroup(capture = false)
                }
                State.lparenQ ->
                    throwIf(true, "unexpected character after (?")
                State.rparen -> {
                    endChoice()
                    with (popGroup()) {
                        start.addCaptureStart(this)
                    }
                }
                State.rparenStar -> {
                    makeRepeat(lazy=false)
                }
                State.rparenStarLazy -> {
                    makeRepeat(lazy=true)
                }
                State.rparenPlus -> {
                    makeRepeat(lazy=false, fromTail=true)
                }
                State.rparenPlusLazy -> {
                    makeRepeat(lazy=true, fromTail=true)
                }
                State.rparenQ -> {
                    makeOptional()
                }
                State.star -> {
                    makeGroupFromAtom()
                    makeRepeat(lazy=false)
                }
                State.query -> {
                    makeGroupFromAtom()
                    makeOptional()
                }
                State.plus -> {
                    makeGroupFromAtom()
                    makeRepeat(lazy=false, fromTail=true)
                }
                State.lazyPlus -> {
                    makeGroupFromAtom()
                    makeRepeat(lazy=true, fromTail=true)
                }
            }
            state = State.none
        }

        root.addTransition(Transition.lambda(newNode()))
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
                        newGroup(root.getNextNodes().first(), capture=false)
                            .also { it.synthetic = true }
                    }
                    with (groupStack.last()) {
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
                    throwIf(state==State.lparenQ, "unexpected character after (?")
                    flush()
                    with(prevNode) {
                        addTransition(Transition.exact(newNode(), ch))
                    }
                }
            }
        }
        flush()
        if (groupStack.isNotEmpty()) {
            throwIf(!popGroup().synthetic, "unmatched left parenthesis")
        }
        prevNode.setTerminal()
        root.getAllNodes().forEach{ it.finalize() }
    }

    /*
     * match - given a string, return either null, for no match, or a (possibly empty)
     * list of captures. Eachcapture is either null, if it matched nothing, or
     * else the last thing it matched.
     *
     * Operation is as follows:
     *
     * 1. Create an initial context starting at the root of the regex.
     * 2. Evaluate the current list of contexts against each character in turn.
     *    A single context can generate more than one next-step contexts if the
     *    regex branches due to either a loop or a choice.
     * 3. At each step, add to the list of contexts all the non-character
     *    transitions (null, repeat, loop exit etc).
     * 4. Repeat the above for each character in turn...
     * 5. ...ending up with a list of contexts that are viable at the end
     *    of the string. Filter these to just those that are marked terminal
     *    (there will only ever be one anyway).
     * 6. Now match the capture groups of the regex against the captures
     *    in the terminal context,to generate the result, i.e. a list
     *    of capture strings.
     * 7. If there was no match, there will either be no remaining contexts,
     *    or theone(s) that remain are not terminal. In that case return null.
     */

    fun match(str: String, verbose: Boolean = false): List<String?>? =
        let {
            val start = root.makeClosure(0, listOf(Context(root, 0, root)))
            str.foldIndexed(start) { index, contexts, ch ->
                (contexts.map { ctx ->
                    ctx.eval(index, ch)
                }.flatten()
                    .groupBy { it.node }
                    .values
                    .map { it.collapse() }
                    .flatten())
                    .also { if (verbose) println("$ch   $it") }
            }.filter { it.node.terminal }.firstOrNull()
                ?.let { result ->
                    groups
                        .filter { g -> g.capture }
                        .map{ g ->
                            result.captures
                                .find{ it.group==g }
                                ?.get(str)}
                }
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
        var groupId = 1
        fun reset() {
            Node.reset()
            groupId = 1
        }

    }
}

fun List<NewRegex.Group>.describe(prefix: String) =
    if (isNotEmpty()) "$prefix${map{"${it.id}"}.joinToString(",")}" else ""