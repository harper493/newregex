const val verbosity = 1
var failures = 0
var tests = setOf<Int>()
val evaluate = true


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

    testRx(3000,"a(b)*c",
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

    testRx(14, "a(bc*)+d",
        listOf( "abd", "abcd", "abccd", "abbd", "abbcd", "abbbcccd"),
        listOf( "ad", "acd" )
    )

    testRx(15, "a(b*c)+d",
        listOf( "acd", "abcd", "abbcd", "accd", "acbcd", "abbbbcbbbbccccd"),
        listOf( "ad", "abd" )
    )

    testRx(16, "a(((b*c)+d?)+e)*f",
        listOf( "af", "acef", "abcdef", "abbbccdcdcecef"),
        listOf( "acf", "abcdf", "abcdebcf" )
    )

    testRx(17, "abc|def",
        listOf( "abc", "def"),
        listOf( "abcdef", "", "ab", "ef" )
    )

    testRx(18, "(abc|def)",
        listOf( "abc", "def"),
        listOf( "abcdef", "", "ab", "ef" )
    )

    testRx(19, "(abc|def)*",
        listOf( "abc", "def", "abcdef", "", "abcdefabc", "abcabcabcdefdef"),
        listOf( "ab", "ef" )
    )

    testRx(20, "((ab)*c|def?)+g",
        listOf( "abcg", "cg", "deg", "defg", "cdefdeg", "ababcdeg" ),
        listOf( "acg", "deffg", "abg", "" )
    )

    testRx(21, ".*foo.*",
        listOf( "foo", "xfooxfoox", "xfoox", "xfooxfooxfooxfooxfooxfooxfooxfooxfooxfooxfooxfoox",
            "reuwiuytruitruiwytwtetuiyrttfwqytwfuitwrertyeuiqetfoeqytwyetqqwfoqweqtyfootqwyfotreqwrefooequy" ),
        listOf( "qtqwetyqewytreqytrqeytqrweytqefoqwyetrqetrqeyqtrweyqtrweqyt" )
    )

    testRx(200, "(x+x+)+y",
        listOf("xxxxxxxxxxy"),
        listOf("xxxxxxxxxx")
    )

    testRx(201, "a(b(c(d)))e",
        listOf(),
        listOf()
    )

    testRx(202, "a(b(c(d)*)*)*e",
        listOf(),
        listOf()
    )

    testRx(203, "((((a)b)c)d)e",
        listOf(),
        listOf()
    )

    testRx(204, "((((a)+b)+c)+d)+e",
        listOf(),
        listOf()
    )

    testRx(205, "x((((a)+b)+c)+d)+e",
        listOf(),
        listOf()
    )

    testRx(206, "a(b|c)*b+d",
        listOf("abcbbd"),
        listOf()
    )

    if (goodTest(100)) {
        for (rxs in listOf(
            "(abc",
            "def)",
            "(?abc"
        )) {
            NewRegex.reset()
            var failed = false
            try {
                val rx = NewRegex(rxs)
            } catch (e: NewRegex.SyntaxException) {
                failed = true
            }
            if (!failed) {
                println("Bad regex '$rxs' not rejected")
                ++failures
            }
        }
    }

    if (failures > 0) {
        println("\n$failures tests failed")
    } else {
        println("\nAll tests passed!")
    }
}

fun makeRx(rxs: String) =
    NewRegex(rxs).also {
        if (verbosity > 0) {
            println("\n$rxs\n${it.show()}")
        }
    }

fun testRx(t: Int, rxstr: String, good: List<String>, bad: List<String>) {
    NewRegex.reset()
    if (goodTest(t)) {
        val rx = makeRx(rxstr)
        if (evaluate) {
            good.forEach { test(t, rx, it, true) }
            bad.forEach { test(t, rx, it, false) }
        }
    }
}

fun goodTest(t: Int) =
    t in tests || tests.isEmpty()

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

