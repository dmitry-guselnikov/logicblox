package net.guselnikov.logicblox.block.parser

import java.math.BigDecimal

private val supportedOperators: List<Operator> = listOf(
    Println, Print, Or, And, Plus, Minus, Div, Mult, Sqrt, Pow, LessOrEqual, GreaterOrEqual, Less, Greater, Equals, NotEquals, Mod, Sin, Cos, Tan, Abs, Ln, Lg, ToInt, Rand
)
private val operationStrings = supportedOperators.map { it.symbols }.flatten().toTypedArray()

private fun String.startsWithOneOf(vararg substring: String): String? {
    substring.forEach {
        if (startsWith(prefix = it, ignoreCase = true)) return it
    }
    return null
}

/**
 * 1. Добавить валидацию на количество скобочек () и {}
 * 2. Добавить валидацию на пересечение блоков «( { ) }»
 * 3.
 */
fun parseCode(code: String): BlockGroup {
    val expressions = arrayListOf<TokenGroup>()
    val tokens = tokens(code)
    var startTokenIndex = 0
    while (true) {
        val chunk = readChunk(tokens, startTokenIndex)
        if (chunk.group.isEmpty()) break
        if (chunk.nextTokenIndex >= tokens.size) break
        startTokenIndex = chunk.nextTokenIndex
        expressions.add(chunk.group)
    }

    return BlockGroup(expressions)
}

class GroupChunk(
    val group: TokenGroup,
    val nextTokenIndex: Int
)

fun readChunk(tokens: List<Token>, startIndex: Int): GroupChunk {
    var chunkStartIndex = startIndex
    while (tokens.getOrNull(chunkStartIndex) == NewLine) {
        chunkStartIndex ++
    }

    if (tokens.getOrNull(chunkStartIndex) == If) {
        return readCondition(tokens, chunkStartIndex)
    }

    return readFormula(tokens, chunkStartIndex)
}

fun readFormula(tokens: List<Token>, startIndex: Int): GroupChunk {
    val formulaTokens = arrayListOf<Token>()
    var nextTokenIndex = startIndex

    run breaking@ {
        tokens.subList(startIndex, tokens.size).forEachIndexed { index, token ->
            nextTokenIndex = startIndex + index
            when (token) {
                is Value, is Word, is Operator, LeftBracket, RightBracket, Assign, Return -> {
                    formulaTokens.add(token)
                }

                is If, is Else, is BlockStart, is BlockEnd -> {
                    return@breaking
                }

                NewLine -> {
                    if (formulaTokens.isNotEmpty() && formulaTokens.last() !is Operator) {
                        return@breaking
                    }
                }
            }
        }
    }

    return GroupChunk(FormulaGroup(formulaTokens), nextTokenIndex)
}

private enum class ConditionReadingMode {
    INIT,
    CONDITION,
    TRUE_STATEMENT,
    TRUE_STATEMENT_BLOCK_STARTED,
    TRUE_STATEMENT_BLOCK_COMPLETED,
    FALSE_STATEMENT,
    FALSE_STATEMENT_BLOCK_STARTED,
    COMPLETED
}

fun readCondition(tokens: List<Token>, startIndex: Int): GroupChunk {
    var mode: ConditionReadingMode = ConditionReadingMode.INIT
    val conditionTokens = arrayListOf<Token>()
    var conditionNesting = 0
    var nextTokenIndex = startIndex
    var trueStatementGroup = BlockGroup(listOf())
    val trueStatementExpressions = arrayListOf<TokenGroup>()
    var falseStatementGroup = BlockGroup(listOf())
    val falseStatementExpressions = arrayListOf<TokenGroup>()
    var skipToIndex = 0

    tokens.subList(startIndex, tokens.size).forEachIndexed { index, token ->
        nextTokenIndex = startIndex + index
        if (nextTokenIndex < skipToIndex) {
            return@forEachIndexed
        }

        when (mode) {
            ConditionReadingMode.INIT -> {
                if (token == If) mode = ConditionReadingMode.CONDITION
            }
            ConditionReadingMode.CONDITION -> {
                if (token == LeftBracket) conditionNesting++
                if (token == RightBracket) conditionNesting--
                conditionTokens.add(token)
                if (conditionNesting == 0) mode = ConditionReadingMode.TRUE_STATEMENT
            }
            ConditionReadingMode.TRUE_STATEMENT -> {
                if (token == BlockStart) mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_STARTED
                else {
                    val trueStatementChunk = readChunk(tokens, nextTokenIndex)
                    trueStatementGroup = BlockGroup(listOf(trueStatementChunk.group))
                    skipToIndex = trueStatementChunk.nextTokenIndex
                    mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED
                }
            }

            ConditionReadingMode.TRUE_STATEMENT_BLOCK_STARTED -> {
                if (token == BlockEnd) {
                    trueStatementGroup = BlockGroup(trueStatementExpressions)
                    mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED
                }
                else {
                    val trueStatementChunk = readChunk(tokens, nextTokenIndex)
                    trueStatementExpressions.add(trueStatementChunk.group)
                    skipToIndex = trueStatementChunk.nextTokenIndex
                }
            }

            ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED -> {
                if (token == NewLine) {
                    return@forEachIndexed
                }

                if (token == Else) {
                    mode = ConditionReadingMode.FALSE_STATEMENT
                } else {
                    return GroupChunk(
                        ConditionGroup(
                            condition = FormulaGroup(conditionTokens),
                            onTrueBlock = trueStatementGroup,
                            onFalseBlock = BlockGroup(listOf())
                        ),
                        nextTokenIndex
                    )
                }
            }

            ConditionReadingMode.FALSE_STATEMENT -> {
                if (token == BlockStart) mode = ConditionReadingMode.FALSE_STATEMENT_BLOCK_STARTED
                else {
                    val falseStatementChunk = readChunk(tokens, nextTokenIndex)
                    falseStatementGroup = BlockGroup(listOf(falseStatementChunk.group))
                    skipToIndex = falseStatementChunk.nextTokenIndex
                    mode = ConditionReadingMode.COMPLETED
                }
            }

            ConditionReadingMode.FALSE_STATEMENT_BLOCK_STARTED -> {
                if (token == BlockEnd) {
                    falseStatementGroup = BlockGroup(falseStatementExpressions)
                    mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED
                }
                else {
                    val falseStatementChunk = readChunk(tokens, nextTokenIndex)
                    falseStatementExpressions.add(falseStatementChunk.group)
                    skipToIndex = falseStatementChunk.nextTokenIndex
                }
            }
            ConditionReadingMode.COMPLETED -> {
                return GroupChunk(
                    ConditionGroup(
                        condition = FormulaGroup(conditionTokens),
                        onTrueBlock = trueStatementGroup,
                        onFalseBlock = falseStatementGroup
                    ),
                    nextTokenIndex
                )
            }
        }
    }

    return GroupChunk(
        ConditionGroup(
            condition = FormulaGroup(conditionTokens),
            onTrueBlock = trueStatementGroup,
            onFalseBlock = falseStatementGroup
        ),
        nextTokenIndex
    )
}

fun tokens(code: String): List<Token> {
    val tokens = arrayListOf<Token>()
    var readingNumber = false
    var readingWord = false
    var readingLiteral = false
    val currentNumber = StringBuilder()
    val currentWord = StringBuilder()
    val currentLiteral = StringBuilder()

    fun writeWord() {
        readingWord = false
        when (val word = currentWord.toString()) {
            "π" -> tokens.add(Number(BigDecimal("3.1415926535897932384626433832795")))
            "true" -> tokens.add(Bool(true))
            "false" -> tokens.add(Bool(false))
            else -> tokens.add(Word(word))
        }
        currentWord.clear()
    }

    fun writeLiteral() {
        readingLiteral = false
        tokens.add(Literal(currentLiteral.toString()))
        currentLiteral.clear()
    }

    fun stopReadings() {
        if (readingWord) {
            writeWord()
        }

        if (readingNumber) {
            readingNumber = false
            tokens.add(Number(BigDecimal(currentNumber.toString())))
            currentNumber.clear()
        }
    }

    var symbolsToSkip = 0
    var skipCurrentLine = false

    code.forEachIndexed { index, symbol ->
        if (symbol == '\n') {
            skipCurrentLine = false
        }

        if (skipCurrentLine) {
            return@forEachIndexed
        }

        if (symbolsToSkip > 0) {
            symbolsToSkip--
            return@forEachIndexed
        }

        val slice = code.slice(index..code.lastIndex)
        val isComment = slice.startsWith("//")
        val isPostScriptum = slice.startsWith("P.S.")
        if (isPostScriptum) return tokens

        if (isComment) {
            skipCurrentLine = true
            return@forEachIndexed
        }

        val tokenStr: String? = slice.startsWithOneOf("if", "else", "return")
        val operatorStr: String? = slice.startsWithOneOf(*operationStrings)

        when {
            symbol == '\"' && readingLiteral -> {
                writeLiteral()
            }

            symbol == '\"' && !readingLiteral -> {
                stopReadings()
                readingLiteral = true
            }

            readingLiteral -> {
                currentLiteral.append(symbol)
            }

            tokenStr != null -> {
                symbolsToSkip = tokenStr.length - 1
                stopReadings()

                when (tokenStr) {
                    "if" -> tokens.add(If)
                    "else" -> tokens.add(Else)
                    "return" -> tokens.add(Return)
                }
            }

            operatorStr != null -> {
                symbolsToSkip = operatorStr.length - 1

                stopReadings()

                val lastToken = tokens.lastOrNull()
                val currentOperator = operatorStr.toOperator()
                when {
                    symbol == '-' && (tokens.isEmpty() || lastToken is Operator || lastToken == LeftBracket || lastToken == Assign) -> tokens.add(UnaryMinus)
                    currentOperator != null -> tokens.add(currentOperator)
                }
            }

            symbol == '\n' -> {
                stopReadings()
                tokens.add(NewLine)
            }

            symbol == '(' -> {
                stopReadings()
                tokens.add(LeftBracket)
            }

            symbol == ')' -> {
                stopReadings()
                tokens.add(RightBracket)
            }

            symbol == '{' -> {
                stopReadings()
                tokens.add(BlockStart)
            }

            symbol == '}' -> {
                stopReadings()
                tokens.add(BlockEnd)
            }

            symbol == '=' -> {
                val variableName = currentWord.toString()
                if (readingNumber || !readingWord || variableName.isBlank()) return listOf()
                writeWord()
                tokens.add(Assign)

                readingWord = false
                currentWord.clear()
            }

            symbol == '.' -> {
                if (!readingNumber) return listOf()
                currentNumber.append(symbol)
            }

            symbol.isDigit() -> {
                if (readingWord) currentWord.append(symbol)
                else {
                    readingNumber = true
                    currentNumber.append(symbol)
                }
            }

            symbol.isLetter() -> {
                if (readingNumber) return listOf()
                readingWord = true
                currentWord.append(symbol)
            }
        }
    }

    if (readingWord) {
        writeWord()
    }

    if (readingNumber) {
        tokens.add(Number(BigDecimal(currentNumber.toString())))
    }

    return tokens
}

fun lines(tokens: List<Token>): List<List<Token>> {
    val lines = arrayListOf<List<Token>>()
    val currentLine = arrayListOf<Token>()

    tokens.forEach { token ->
        if (token != NewLine) {
            currentLine.add(token)
        } else {
            if (currentLine.isNotEmpty()) {
                val buffer = arrayListOf<Token>()
                buffer.addAll(currentLine)
                lines.add(buffer)
                currentLine.clear()
            }
        }
    }

    return lines
}