package me.anno.knoq

import kotlin.collections.set

data class Rule(val head: Expr, val body: Expr) {

    val reverse get() = Rule(body, head)

    fun applyAll(expr: Expr): Expr {
        val match = patternMatch(head, expr)
        if (match != null) return replace(body, match)
        return if (expr is Func)
            Func(expr.name, expr.args.map { applyAll(it) })
        else expr
    }

    fun applyNth(expr: Expr, n: Int) = applyNth(expr, intArrayOf(n))

    fun applyAll2(expr: Expr): List<Expr> {
        val match = patternMatch(head, expr)
        return if (expr is Func) {
            val result = ArrayList<Expr>()
            if (match != null) result.add(replace(body, match))
            val args = expr.args
            val array = args.toTypedArray()
            for (i in array.indices) {
                val subSolutions = applyAll2(array[i])
                for (si in subSolutions.indices) {
                    array[i] = subSolutions[si]
                    result.add(Func(expr.name, array.toList()))
                }
            }
            result
        } else {
            if (match != null) listOf(replace(body, match))
            else emptyList()
        }
    }

    private fun applyNth(expr: Expr, n: IntArray): Expr {
        if (n[0] < 0) return expr
        val match = patternMatch(head, expr)
        if (match != null) {
            if (n[0]-- == 0) {
                return replace(body, match)
            }
        }
        return if (expr is Func)
            Func(expr.name, expr.args.map { applyNth(it, n) })
        else expr
    }

    private fun replace(expr: Expr, bindings: Map<String, Expr>): Expr {
        if (expr is String) return bindings[expr] ?: expr
        if (expr is Int) return expr
        expr as Func
        return Func(expr.name, expr.args.map {
            replace(it, bindings)
        })
    }

    fun patternMatch(pattern: Expr, value: Expr, bindings: HashMap<String, Expr> = HashMap()): Map<String, Expr>? {
        return when (pattern) {
            is String -> {
                if (pattern !in bindings || bindings[pattern] == value) {
                    bindings[pattern] = value
                    bindings
                } else null // no match
            }
            is Int -> if (value == pattern) bindings else null
            is Func -> {
                if (value is Func &&
                    pattern.name == value.name &&
                    pattern.args.size == value.args.size
                ) {
                    for (i in pattern.args.indices) {
                        if (patternMatch(pattern.args[i], value.args[i], bindings) == null) {
                            return null
                        }
                    }
                    bindings
                } else null
            }
            else -> null
        }
    }


}