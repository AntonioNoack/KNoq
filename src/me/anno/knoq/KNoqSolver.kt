package me.anno.knoq

import me.anno.Engine
import me.anno.utils.strings.StringHelper.levenshtein
import java.util.*

fun findIsEqual(rules: List<Rule>, input: String, output: String, maxTries: Int) =
    findIsEqual(rules, KNoqLang.parse(input), KNoqLang.parse(output), maxTries)

fun findIsEqual(rules: List<Rule>, input: Expr, output: Expr, maxTries: Int): List<Expr>? {

    class NewExpr(val value: Expr, val cost: Int, val depth: Int) {
        operator fun component1() = value
        operator fun component2() = cost
        operator fun component3() = depth
    }

    fun Expr.length(): Int {
        return when (this) {
            is String -> length
            is Func -> size
            else -> toString().length
        }
    }

    val added = HashSet<Expr>()
    val map = HashMap<Expr, Expr>()
    val todo = PriorityQueue<NewExpr> { a, b ->
        val c = a.cost.compareTo(b.cost)
        if (c == 0) a.value.toString().compareTo(b.value.toString()) else c
    }

    todo.add(NewExpr(input, 0, 0))
    added.add(input)

    var maxCost = 10
    var lastTime = Engine.nanoTime

    val outputStr = output.toString()

    search@ while (todo.isNotEmpty()) {
        val (term, cost, depth) = todo.poll()
        if (cost >= 2 * maxCost || (Engine.nanoTime > 1e9 + lastTime)) {
            maxCost = cost
            lastTime = Engine.nanoTime
            println("checking cost $cost, ${added.size} checked, $term")
        }
        for (rule in rules) {
            val all = rule.applyAll2(term)
            val numRuleMatches = all.size
            for (i in all.indices) {
                val newTerm = all[i]
                if (newTerm == output) {
                    println("found path after ${added.size} entries, cost $cost")
                    // found path :)
                    val answer = ArrayList<Expr>()
                    answer.add(newTerm)
                    var path = term
                    while (true) {
                        answer.add(path)
                        path = map[path] ?: break
                    }
                    answer.reverse()
                    return answer
                }
                if (added.add(newTerm)) {
                    map[newTerm] = term
                    val newCost = depth + // ancestry cost
                            newTerm.length() + // cost for length
                            1 + // base cost
                            numRuleMatches + // rules that can be applied everywhere should be less desirable // 731,502 -> 601,438
                            // cost for having a different structure from the target
                            // 13615,1058 -> 1832,1218 (so yes, helps sometimes)
                            newTerm.toString().levenshtein(outputStr, ignoreCase = false)
                    todo.add(NewExpr(newTerm, newCost, depth + 1))
                }
            }
            if (added.size > maxTries) {
                println("exceeded trial limit of $maxTries, last cost: $cost")
                return null
            }
        }
    }
    return null
}
