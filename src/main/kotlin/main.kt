val verbosity = 1
var failures = 0
var tests = setOf<Int>(9)


fun main(args: Array<String>) {
    val rx1 = makeRx(1,"abc")
    val m1 = test(rx1, "abc")
    val m2 = test(rx1, "def", false)
    val m3 = test(rx1, "abd", false)
    val m4 = test(rx1, "abcd", false)
    val m5 = testF(rx1,"ab")

    val rx2 = makeRx(2,"a.c")
    val m11 = test(rx2, "abc")
    val m12 = test(rx2, "axc")
    val m13 = test(rx2, "axxc", false)
    val m14 = test(rx2, "zbc", false)
    val m15 = test(rx2, "ac", false)

    val rx3 = makeRx(3,"ab*c")
    val m21 = test(rx3, "ac")
    val m22 = test(rx3, "abc")
    val m23 = test(rx3, "abbc")
    val m24 = test(rx3, "abbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbc")
    val m25 = test(rx3, "axc", false)
    val m26 = test(rx3, "axxc", false)
    val m27 = test(rx3, "zbc", false)
    val m28 = test(rx3, "ab", false)
    val m29 = test(rx3, "abx", false)

    val rx4 = makeRx(4,"a(bc)*d")
    val m30 = test(rx4, "ad")
    val m31 = test(rx4, "abcd")
    val m32 = test(rx4, "abcbcbcd")
    val m33 = testF(rx4, "abd")
    val m34 = testF(rx4, "abcbd")
    val m35 = testF(rx4, "abcbcx")
    val m36 = testF(rx4, "axbcd")
    val m37 = testF(rx4, "abcxd")
    
    val rx5 = makeRx(5,"a.*b")
    val m40 = test(rx5, "ab")
    val m41 = test(rx5, "axb")
    val m42 = test(rx5, "axyzb")
    val m43 = testF(rx5, "az")
    val m44 = testF(rx5, "axyz")
    val m45 = testF(rx5, "xyzb")

    val rx6 = makeRx(6, "a(b.*d)*e")
    val m50 = test(rx6, "ae")
    val m51 = test(rx6, "abde")
    val m52 = test(rx6, "abxde")
    val m53 = test(rx6, "abxxxxxdbdbdbde")
    val m54 = testF(rx6, "abxdbyyye")
    val m55 = testF(rx6, "axde")

    val rx7 = makeRx(7, "ab?c")
    val m60 = test(rx7, "ac")
    val m61 = test(rx7, "abc")
    val m62 = testF(rx7, "abbc")
    val m63 = testF(rx7, "ad")
    val m64 = testF(rx7, "abd")
    val m65 = testF(rx7, "axbc")

    val rx8 = makeRx(8, "a(bc)?d")
    val m70 = test(rx8,"ad")
    val m71 = test(rx8, "abcd")
    val m72 = testF(rx8, "abd")
    val m73 = testF(rx8, "abc")
    val m74 = testF(rx8, "abcx")

    val rx9 = makeRx(9, "a(bc?d)*e")
    val m80 = test(rx9, "ae")
    val m81 = test(rx9, "abde")
    val m82 = test(rx9, "abcde")
    val m83 = test(rx9, "abcdbdbcde")
    val m84 = testF(rx9, "abcdbd")


    if (failures > 0) {
        println("\n$failures tests failed")
    } else {
        println("All tests passed!")
    }
}

fun makeRx(t: Int, rxs: String) =
    run {
        if (t in tests || tests.isEmpty()) {
            val rx = NewRegex(rxs)
            if (verbosity > 0) {
                println(rx.show())
            }
            rx
        } else {
            null
        }
    }

fun test(rx: NewRegex?, s: String, expected: Boolean=true) =
    run {
        val result = rx?.match(s, verbose=verbosity > 1) ?: expected
        if (result!=expected) {
            println("Wrong result for '$s': got $result, expected $expected")
            ++failures
        }
        result
    }

fun testF(rx: NewRegex?, s: String) =
    test(rx, s, false)
