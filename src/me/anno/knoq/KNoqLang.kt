package me.anno.knoq

import me.anno.ui.editor.code.codemirror.*
import me.anno.utils.structures.arrays.LineSequence
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Strings.isBlank2
import me.anno.utils.types.Strings.isName
import me.anno.utils.types.Strings.isNumber
import java.io.IOException

/**
 * parser, tokenizer, and formatter
 * */
object KNoqLang : Language {

    const val operators = "+-*/^"

    fun operatorPrecedence(symbol: String): Int {
        return when (symbol) {
            "^" -> 0
            "*", "/" -> 1
            "+", "-" -> 2
            else -> throw NotImplementedError()
        }
    }

    private fun isExpr(expr: Any): Boolean {
        return (expr is String && isName(expr)) || expr is Int || expr is Func
    }

    private fun tokenize(text: String): List<Any> {
        val s = LineSequence()
        s.setText(text)
        val stream = Stream(s)
        val tokens = ArrayList<Any>()
        val state = getStartState()
        while (stream.index < text.length) {
            getToken(stream, state)
            if (stream.startIndex == stream.index) break
            val term = text.substring(stream.startIndex, stream.index).trim()
            tokens.add(if (isNumber(term)) term.toInt() else term)
            stream.startIndex = stream.index
        }
        tokens.removeIf { it is String && it.isBlank2() }
        return tokens
    }

    fun parse(text: String): Expr {
        var sequence = tokenize(text)
        simplification@ while (sequence.size > 1) {
            for (i in 0 until sequence.size - 2) {
                val name = sequence[i]
                if (name is String && isName(name) && sequence[i + 1] == "(") {
                    // find all arguments
                    var length = 0
                    args@ for (j in i + 2 until sequence.size step 2) {
                        if (isExpr(sequence[j])) {
                            // fine :)
                            when (sequence[j + 1]) {
                                "," -> length++
                                ")" -> {
                                    length++
                                    break@args
                                }
                                else -> { // invalid
                                    length = -1
                                    break@args
                                }
                            }
                        } else { // invalid
                            length = -1
                            break@args
                        }
                    }
                    if (length >= 0) {
                        val func = Func(name, (0 until length).map { sequence[i + 2 + it * 2] })
                        sequence = sequence.subList(0, i) + func + sequence.subList(i + 2 + length * 2, sequence.size)
                        continue@simplification
                    } // else skip, not ready yet
                }
            }
            // ^
            for (i in 0 until sequence.size - 2) {
                val symbol = sequence[i + 1]
                if (symbol == "^" && isExpr(sequence[i]) && isExpr(sequence[i + 2])) {
                    val func = Func(sequence[i + 1] as String, listOf(sequence[i], sequence[i + 2]))
                    sequence = sequence.subList(0, i) + func + sequence.subList(i + 3, sequence.size)
                    continue@simplification
                }
            }
            // */
            for (i in 0 until sequence.size - 2) {
                val symbol = sequence[i + 1]
                if ((symbol == "*" || symbol == "/") && isExpr(sequence[i]) && isExpr(sequence[i + 2])) {
                    val func = Func(sequence[i + 1] as String, listOf(sequence[i], sequence[i + 2]))
                    sequence = sequence.subList(0, i) + func + sequence.subList(i + 3, sequence.size)
                    continue@simplification
                }
            }
            // +-
            for (i in 0 until sequence.size - 2) {
                val symbol = sequence[i + 1]
                if ((symbol == "+" || symbol == "-") && isExpr(sequence[i]) && isExpr(sequence[i + 2])) {
                    val func = Func(sequence[i + 1] as String, listOf(sequence[i], sequence[i + 2]))
                    sequence = sequence.subList(0, i) + func + sequence.subList(i + 3, sequence.size)
                    continue@simplification
                }
            }
            for (i in 0 until sequence.size - 2) {
                if (sequence[i] == "(" && isExpr(sequence[i + 1]) && sequence[i + 2] == ")") {
                    sequence = sequence.subList(0, i) + sequence[i + 1] + sequence.subList(i + 3, sequence.size)
                    continue@simplification
                }
            }
            if (sequence.size == 3) {
                if (isExpr(sequence[0]) && sequence[1] == "=" && isExpr(sequence[2])) {
                    return Rule(sequence[0], sequence[2])
                }
            }
            throw IOException("Cannot simplify $sequence")
        }
        return sequence[0]
    }

    override val blockCommentStart = "/*"
    override val blockCommentEnd = "*/"
    override val electricInput = LuaLanguage().electricInput
    override val lineComment = "//"

    private fun fullMatch(vararg list: String): Collection<String> {
        return if (list.size < 16) list.toList() else list.toHashSet()
    }

    private fun partialMatch(vararg list: String): (CharSequence) -> Boolean {
        return { tested -> list.any { pattern -> pattern.startsWith(tested) } }
    }

    private val keywords = fullMatch()
    private val indentTokens = fullMatch("\\(", "{")
    private val dedentTokens = fullMatch("\\)", "}")
    private val dedentPartial = partialMatch("\\)", "}")

    override fun getIndentation(state: State, indentUnit: Int, textAfter: CharSequence): Int {
        val isClosing = dedentPartial(textAfter)
        return state.indent0 + indentUnit * (state.indentDepth - isClosing.toInt())
    }

    override fun getStartState(): State {
        return State(0, 0) { stream, _ ->
            when (stream.next()) {
                in '0'..'9' -> {
                    stream.eatWhile { it in '0'..'9' }
                    TokenType.NUMBER
                }
                in 'A'..'Z', in 'a'..'z' -> {
                    stream.eatWhile { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' }
                    TokenType.VARIABLE
                }
                '(', ')' -> TokenType.BRACKET
                '=', '*', '+', '-', '/', '^' -> TokenType.OPERATOR
                else -> TokenType.UNKNOWN
            }
        }
    }

    override fun getToken(stream: Stream, state: State): TokenType {
        if (stream.eatSpace()) return TokenType.UNKNOWN
        var style = state.cur(stream, state)
        val word = stream.current().toString()
        if (style == TokenType.VARIABLE) {
            if (keywords.contains(word)) style = TokenType.KEYWORD
        }
        if (style != TokenType.COMMENT && style != TokenType.STRING) {
            when {
                indentTokens.contains(word) -> state.indentDepth++
                dedentTokens.contains(word) -> state.indentDepth--
            }
        }
        return style
    }

}