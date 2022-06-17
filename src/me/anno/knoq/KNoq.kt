package me.anno.knoq

import me.anno.knoq.KNoqLang.parse

typealias Expr = Any

fun printIsEqual(rules: List<Rule>, input: String, output: String, maxTries: Int = 100_000) {
    val result = findIsEqual(rules, input, output, maxTries)
    if (result != null) {
        println("--- $input = $output ---")
        for ((index, termI) in result.withIndex()) {
            println("($index) $termI")
        }
        println("--- $input = $output ---")
    } else {
        println("--- $input =?= $output ---")
    }
}

fun main() {

    var rules = listOf(
        "swap(pair(a,b))=pair(b,a)",
        "2=1+1",
        "3=2+1",
        "4=3+1",
        "a+b=b+a",
        "a+0=0+a",
        "a*b=b*a",
        "a+a=2*a",
        // "a*1=a",
        "s(a)=a+1", // the engine needs much longer with this rule...
        "(a+b)+c=a+(b+c)",
        "a^2=a*a",
        "a*(b+c)=a*b+a*c",
    ).map { parse(it) as Rule }

    rules = rules + rules.map { it.reverse }

    printIsEqual(rules, "(a+b)^2", "a^2+2*a*b+b^2")
    printIsEqual(rules, "4", "4*1")
    printIsEqual(rules, "3", "1+1")

    val testRule = parse("swap(pair(a,b))=pair(b,a)") as Rule
    val testExpr = parse("swap(pair(p,q))")

    println(parse("a+b*c-d*a^2"))

    // parser & rule application testing
    println(testRule.patternMatch(testRule.head, testExpr))
    println(testRule.applyAll(testExpr))
    println(testRule.applyNth(testExpr, 0))
    println(testRule.applyNth(testExpr, 1))
    println(testRule.reverse.applyAll(testExpr))

    // to do program editor, where we can type in noq...
    // to do maybe a command line like Tsoding
    // not really needed, because we can quickly prototype here :)
    /*GFX.disableRenderDoc = true
    testUI {
        val editor = CodeEditor(style)
        addEvent { editor.requestFocus() }
        editor
    }*/

}