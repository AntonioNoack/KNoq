package me.anno.knoq

import me.anno.knoq.KNoqLang.operatorPrecedence
import me.anno.knoq.KNoqLang.operators
import me.anno.utils.types.Booleans.toInt

data class Func(val name: String, val args: List<Expr>) {

    companion object {
        private val sizeTable = intArrayOf(9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999, Int.MAX_VALUE)
        private fun stringSize(var0: Int): Int {
            var var1 = 0
            while (var0 > sizeTable[var1]) {
                var1++
            }
            return var1 + 1
        }
    }

    // for debugging originally
    // constructor(name: String, vararg args: Expr) : this(name, args.toList())

    private fun append(str: java.lang.StringBuilder, a: Expr) {
        // only place brackets if needed
        if (a is Func && a.name in operators && operatorPrecedence(a.name) >= operatorPrecedence(name)) {
            str.append('(').append(a).append(')')
        } else {
            str.append(a)
        }
    }

    private fun calcSize(a: Expr): Int {
        // only place brackets if needed
        return if (a is Func)
            a.size + (a.name in operators && operatorPrecedence(a.name) >= operatorPrecedence(name)).toInt(2)
        else if (a is String) a.length
        else if (a is Int)
            if (a < 0) stringSize(-a) + 1 else stringSize(a)
        else throw NotImplementedError()
    }

    private val formatted = if (name.length == 1 && name in operators) {
        val str = StringBuilder(name.length + calcSize(args[0]) + calcSize(args[1]))
        append(str, args[0])
        str.append(name)
        append(str, args[1])
        str.toString()
    } else "$name(${args.joinToString()})"

    val size = formatted.length

    override fun toString() = formatted

}