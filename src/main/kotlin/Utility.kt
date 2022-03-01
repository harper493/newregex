fun<T> iterativeClosure(coll: List<T>, fn: (T)->List<T>): List<T> {
    val next = coll.map { fn(it) }.flatten()
    return if (next.isNotEmpty()) {
        (coll + iterativeClosure(next, fn))
    } else {
        coll
    }
}

