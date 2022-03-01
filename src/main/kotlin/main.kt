val verbosity = 0
var failures = 0
var tests = setOf<Int>()


fun main(args: Array<String>) {

    testRx(1, "abc",
        listOf("abc"),
        listOf("def", "abd", "abcd", "ab")
    )

    testRx(2,"a.c",
        listOf("abc", "axc"),
        listOf("axxc",  "zbc",  "ac")
    )

    testRx(3,"ab*c",
        listOf("ac", "abc", "abbc", "abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbc"),
        listOf("axc",  "axxc",  "zbc",  "ab",  "abx")
    )

    testRx(4, "a(bc)*d",
        listOf("ad", "abcd", "abcbcbcd"),
        listOf("abd",  "abcbd",  "abcbcx",  "axbcd",  "abcxd")
    )

    testRx(5, "a.*b",
        listOf("ab", "axb", "axyzb"),
        listOf("az",  "axyz",  "xyzb")
    )

    testRx(6, "a(b.*d)*e",
        listOf("ae", "abde", "abxde", "abxxxxxdbdbdbde"),
        listOf("abxdbyyye",  "axde")
    )

    testRx(7, "ab?c",
        listOf("ac", "abc"),
        listOf("abbc",  "ad",  "abd",  "axbc")
    )

    testRx(8, "a(bc)?d",
        listOf("ad", "abcd"),
        listOf("abd",  "abc",  "abcx")
    )

    testRx(9, "a(bc?d)*e",
        listOf( "ae", "abde", "abcde", "abcdbdbcde"),
        listOf( "abcdbd")
    )

    testRx(10, "ab+c",
        listOf( "abc", "abbc", "abbbbbbc"),
        listOf( "ac", "axc", "abbd", "axbbbc", "abbxc")
    )

    testRx(11, "ab*c*d",
        listOf( "ad", "abcd", "abd", "acd", "abbbccd"),
        listOf( "ac", "ab", "acbd", "abbccbd")
    )

    testRx(12, "a(bc*)*d",
        listOf( "ad", "abd", "abcd", "abbcd", "abccd", "abbbd", "abcbcbcbd"),
        listOf( "ax", "acd", "accd" )
    )

    testRx(13, "a(bc+)*d",
        listOf( "ad", "abcd", "abccd", "abcbccd"),
        listOf( "abd", "abcbd", "axbcd", "abxd")
    )

    if (failures > 0) {
        println("\n$failures tests failed")
    } else {
        println("All tests passed!")
    }
}

fun makeRx(t: Int, rxs: String) =
    NewRegex(rxs).also {
        if (verbosity > 0) {
            println("\n$rxs\n${it.show()}")
        }
    }

fun testRx(t: Int, rxstr: String, good: List<String>, bad: List<String>) {
    if (t in tests || tests.isEmpty()) {
        val rx = makeRx(t, rxstr)
        good.forEach { test(t, rx, it, true) }
        bad.forEach { test(t, rx, it, false) }
    }
}

fun test(t: Int, rx: NewRegex, s: String, expected: Boolean=true) =
    run {
        if (verbosity>1) println("\n\"$s\"")
        val result = rx.match(s, verbose=verbosity > 1)
        if (result!=expected) {
            println("Wrong result for test $t '$s': got $result, expected $expected")
            ++failures
        }
        result
    }

