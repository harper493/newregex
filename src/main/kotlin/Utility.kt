fun<T> iterativeClosure(coll: List<T>, fn: (T)->List<T>): List<T> {
        val next = coll.map { fn(it) }.flatten()
        if (next.isNotEmpty()) {
            return (coll + iterativeClosure(next, fn))
        } else {
            return coll
        }
    }

