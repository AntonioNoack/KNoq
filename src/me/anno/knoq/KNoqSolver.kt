package me.anno.knoq

import me.anno.Engine
import java.util.*

fun findIsEqual(rules: List<Rule>, input: String, output: String, maxTries: Int) =
    findIsEqual(rules, KNoqLang.parse(input), KNoqLang.parse(output), maxTries)

fun findIsEqual(rules: List<Rule>, input: Expr, output: Expr, maxTries: Int): List<Expr>? {

    fun Expr.length(): Int {
        return when (this) {
            is String -> length
            is Func -> size
            else -> toString().length
        }
    }

    val added = HashSet<Expr>()
    val map = HashMap<Expr, Expr>()
    val todo = PriorityQueue<Pair<Expr, Int>> { a, b ->
        val c = a.second.compareTo(b.second)
        if (c == 0) a.first.toString().compareTo(b.first.toString()) else c
    }

    todo.add(input to 0)
    added.add(input)

    var maxCost = 10
    var lastTime = Engine.nanoTime
    val slow = false

    fun checkTerm(term: Expr, newTerm: Expr, cost: Int, selfCost: Int, i: Int): List<Expr>? {
        if (newTerm == output) {
            println("found path after ${added.size} entries")
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
            todo.add(newTerm to (cost - selfCost + newTerm.length() + 1 + 5 * i))
        }
        return null
    }

    search@ while (todo.isNotEmpty()) {
        val (term, cost) = todo.poll()
        val selfCost = term.length()
        if (cost >= 2 * maxCost || (Engine.nanoTime > 1e9 + lastTime)) {
            maxCost = cost
            lastTime = Engine.nanoTime
            println("checking cost $cost, ${added.size} checked, $term")
        }
        for (rule in rules) {
            if (slow) {// O(n²), so potentially slow with long formulas and generic rules
                var i = 0
                while (true) {
                    val newTerm = rule.applyNth(term, i)
                    if (newTerm == term) break // nothing changed -> end of rules
                    val answer = checkTerm(term, newTerm, cost, selfCost, i)
                    if (answer != null) return answer
                    i++
                }
            } else {// O(n)
                val all = rule.applyAll2(term)
                for (i in all.indices) {
                    val newTerm = all[i]
                    val answer = checkTerm(term, newTerm, cost, selfCost, i)
                    if (answer != null) return answer
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
