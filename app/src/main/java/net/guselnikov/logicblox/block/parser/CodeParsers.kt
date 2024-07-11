package net.guselnikov.logicblox.block.parser

import java.math.BigDecimal

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
suspend fun parseCode(code: String): BlockGroup {
    val expressions = arrayListOf<TokenGroup>()
    val tokens = tokens(code)
    var startTokenIndex = 0
    while (true) {
        val chunk = readChunk(tokens, startTokenIndex)
        if (chunk.group.isEmpty()) break

        expressions.add(chunk.group)
        if (chunk.lastUsedTokenIndex >= tokens.lastIndex) break

        startTokenIndex = chunk.lastUsedTokenIndex + 1
    }

    return BlockGroup(expressions)
}

class GroupChunk(
    val group: TokenGroup,
    val lastUsedTokenIndex: Int
)

fun readChunk(tokens: List<Token>, startIndex: Int): GroupChunk {
    var chunkStartIndex = startIndex
    while (tokens.getOrNull(chunkStartIndex) == NewLine) {
        chunkStartIndex++
    }

    if (tokens.getOrNull(chunkStartIndex) == If) {
        return readCondition(tokens, chunkStartIndex)
    }

    if (tokens.getOrNull(chunkStartIndex) == While) {
        return readWhileLoop(tokens, chunkStartIndex)
    }

    if (tokens.getOrNull(chunkStartIndex) == For) {
        return readForLoop(tokens, chunkStartIndex)
    }

    return readFormula(tokens, chunkStartIndex)
}

fun readFormula(tokens: List<Token>, startIndex: Int): GroupChunk {
    val formulaTokens = arrayListOf<Token>()
    var nextTokenIndex = startIndex

    run breaking@{
        tokens.subList(startIndex, tokens.size).forEachIndexed { index, token ->
            nextTokenIndex = startIndex + index
            when (token) {
                is Value, is Word, is Operator, LeftBracket, RightBracket, Assign, Return, Break, Continue -> {
                    formulaTokens.add(token)
                }

                is While, If, is Else, is BlockStart, is BlockEnd, For, From, To, Step -> {
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
                // TODO: Сделать скобки необязательными!
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
                    skipToIndex = trueStatementChunk.lastUsedTokenIndex
                    mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED
                }
            }

            ConditionReadingMode.TRUE_STATEMENT_BLOCK_STARTED -> {
                if (token == BlockEnd) {
                    trueStatementGroup = BlockGroup(trueStatementExpressions)
                    mode = ConditionReadingMode.TRUE_STATEMENT_BLOCK_COMPLETED
                } else {
                    val trueStatementChunk = readChunk(tokens, nextTokenIndex)
                    trueStatementExpressions.add(trueStatementChunk.group)
                    skipToIndex = trueStatementChunk.lastUsedTokenIndex
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
                        nextTokenIndex - 1
                    )
                }
            }

            ConditionReadingMode.FALSE_STATEMENT -> {
                if (token == BlockStart) mode = ConditionReadingMode.FALSE_STATEMENT_BLOCK_STARTED
                else {
                    val falseStatementChunk = readChunk(tokens, nextTokenIndex)
                    falseStatementGroup = BlockGroup(listOf(falseStatementChunk.group))
                    skipToIndex = falseStatementChunk.lastUsedTokenIndex
                    mode = ConditionReadingMode.COMPLETED
                }
            }

            ConditionReadingMode.FALSE_STATEMENT_BLOCK_STARTED -> {
                if (token == BlockEnd) {
                    falseStatementGroup = BlockGroup(falseStatementExpressions)
                    mode = ConditionReadingMode.COMPLETED
                } else {
                    val falseStatementChunk = readChunk(tokens, nextTokenIndex)
                    falseStatementExpressions.add(falseStatementChunk.group)
                    skipToIndex = falseStatementChunk.lastUsedTokenIndex
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

private enum class WhileLoopReadingMode {
    INIT,
    CONDITION,
    STATEMENT,
    STATEMENT_BLOCK_STARTED,
    COMPLETED
}

fun readWhileLoop(tokens: List<Token>, startIndex: Int): GroupChunk {
    var mode = WhileLoopReadingMode.INIT
    val conditionTokens = arrayListOf<Token>()
    var conditionNesting = 0
    var nextTokenIndex = startIndex
    var statementGroup = BlockGroup(listOf())
    var statementExpressions = arrayListOf<TokenGroup>()
    var skipToIndex = 0
    val tokensSublist = tokens.subList(startIndex, tokens.size)

    tokensSublist.forEachIndexed { index, token ->
        nextTokenIndex = startIndex + index
        if (nextTokenIndex < skipToIndex) {
            return@forEachIndexed
        }

        when (mode) {

            WhileLoopReadingMode.INIT -> {
                if (token == While) mode = WhileLoopReadingMode.CONDITION
            }

            WhileLoopReadingMode.CONDITION -> {
                if (token == LeftBracket) conditionNesting++
                if (token == RightBracket) conditionNesting--
                conditionTokens.add(token)
                // TODO: Сделать скобки необязательными!
                if (conditionNesting == 0) mode = WhileLoopReadingMode.STATEMENT
            }

            WhileLoopReadingMode.STATEMENT -> {
                when (token) {
                    BlockStart -> mode = WhileLoopReadingMode.STATEMENT_BLOCK_STARTED
                    NewLine -> return@forEachIndexed
                    else -> {
                        val loopChunk = readChunk(tokens, nextTokenIndex)
                        statementGroup = BlockGroup(listOf(loopChunk.group))
                        skipToIndex = loopChunk.lastUsedTokenIndex
                        mode = WhileLoopReadingMode.COMPLETED
                    }
                }
            }

            WhileLoopReadingMode.STATEMENT_BLOCK_STARTED -> {
                when (token) {
                    BlockEnd -> {
                        statementGroup = BlockGroup(statementExpressions)
                        mode = WhileLoopReadingMode.COMPLETED
                    }
                    NewLine -> {
                        return@forEachIndexed
                    }
                    else -> {
                        val statementChunk = readChunk(tokens, nextTokenIndex)
                        statementExpressions.add(statementChunk.group)
                        skipToIndex = statementChunk.lastUsedTokenIndex
                    }
                }
            }

            WhileLoopReadingMode.COMPLETED -> {
                return GroupChunk(
                    WhileLoopGroup(
                        condition = FormulaGroup(conditionTokens),
                        loopBlock = statementGroup
                    ),
                    nextTokenIndex
                )
            }
        }
    }

    return GroupChunk(
        WhileLoopGroup(
            condition = FormulaGroup(conditionTokens),
            loopBlock = statementGroup
        ), nextTokenIndex
    )
}

// for (i from 1 to 10 step 0.5)
private enum class ForLoopReadingMode {
    INIT,
    VARIABLE,
    VARIABLE_READ,
    START_VALUE,
    END_VALUE,
    STEP,
    STATEMENT,
    STATEMENT_BLOCK_STARTED,
    COMPLETED
}

// for (i from 1 to 10 step 0.5)
fun readForLoop(tokens: List<Token>, startIndex: Int): GroupChunk {
    var mode = ForLoopReadingMode.INIT
    var iterationVariable: String = ""

    val startValueTokens = arrayListOf<Token>()
    val endValueTokens = arrayListOf<Token>()
    val stepValueTokens = arrayListOf<Token>()

    var nextTokenIndex = startIndex
    var statementExpressions = arrayListOf<TokenGroup>()
    var skipToIndex = 0
    val tokensSublist = tokens.subList(startIndex, tokens.size)

    tokensSublist.forEachIndexed { index, token ->
        nextTokenIndex = startIndex + index
        if (nextTokenIndex < skipToIndex) {
            return@forEachIndexed
        }

        when (mode) {
            ForLoopReadingMode.INIT -> {
                if (token == For) mode = ForLoopReadingMode.VARIABLE
            }
            ForLoopReadingMode.VARIABLE -> {
                if (token is Word) {
                    iterationVariable = token.string
                    mode = ForLoopReadingMode.VARIABLE_READ
                }
            }
            ForLoopReadingMode.VARIABLE_READ -> {
                if (token == From) {
                    mode = ForLoopReadingMode.START_VALUE
                }
            }
            ForLoopReadingMode.START_VALUE -> {
                if (token == To) mode = ForLoopReadingMode.END_VALUE
                else startValueTokens.add(token)
            }
            ForLoopReadingMode.END_VALUE -> {
                when (token) {
                    Step -> mode = ForLoopReadingMode.STEP
                    RightBracket -> mode = ForLoopReadingMode.STATEMENT
                    else -> endValueTokens.add(token)
                }
            }
            ForLoopReadingMode.STEP -> {
                var nesting = 1
                if (token == LeftBracket) nesting++
                if (token == RightBracket) nesting--

                if (nesting == 0) mode = ForLoopReadingMode.STATEMENT
                else stepValueTokens.add(token)
            }
            ForLoopReadingMode.STATEMENT -> {
                when (token) {
                    BlockStart -> mode = ForLoopReadingMode.STATEMENT_BLOCK_STARTED
                    NewLine -> return@forEachIndexed
                    else -> {
                        val loopChunk = readChunk(tokens, nextTokenIndex)
                        statementExpressions.add(loopChunk.group)
                        skipToIndex = loopChunk.lastUsedTokenIndex
                        mode = ForLoopReadingMode.COMPLETED
                    }
                }
            }
            ForLoopReadingMode.STATEMENT_BLOCK_STARTED -> {
                when (token) {
                    BlockEnd -> {
                        mode = ForLoopReadingMode.COMPLETED
                    }
                    NewLine -> return@forEachIndexed
                    else -> {
                        val statementChunk = readChunk(tokens, nextTokenIndex)
                        statementExpressions.add(statementChunk.group)
                        skipToIndex = statementChunk.lastUsedTokenIndex
                    }
                }
            }
            ForLoopReadingMode.COMPLETED -> {
                return GroupChunk(
                    ForLoopGroup(
                        variable = iterationVariable,
                        loopBlock = BlockGroup(statementExpressions),
                        start = FormulaGroup(startValueTokens),
                        end = FormulaGroup(endValueTokens),
                        step = if (stepValueTokens.isNotEmpty()) FormulaGroup(stepValueTokens) else FormulaGroup(
                            listOf(Number(BigDecimal.ONE))
                        )
                    ),
                    nextTokenIndex
                )
            }
        }
    }

    return GroupChunk(
        ForLoopGroup(
            variable = iterationVariable,
            loopBlock = BlockGroup(statementExpressions),
            start = FormulaGroup(startValueTokens),
            end = FormulaGroup(endValueTokens),
            step = if (stepValueTokens.isNotEmpty()) FormulaGroup(stepValueTokens) else FormulaGroup(
                listOf(Number(BigDecimal.ONE))
            )
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

        val tokenStr: String? = slice.startsWithOneOf("if", "else", "return", "while", "break", "continue", "for", "to", "from", "step")
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
                    "while" -> tokens.add(While)
                    "if" -> tokens.add(If)
                    "else" -> tokens.add(Else)
                    "return" -> tokens.add(Return)
                    "break" -> tokens.add(Break)
                    "continue" -> tokens.add(Continue)
                    "for" -> tokens.add(For)
                    "to" -> tokens.add(To)
                    "from" -> tokens.add(From)
                    "step" -> tokens.add(Step)
                }
            }

            operatorStr != null -> {
                symbolsToSkip = operatorStr.length - 1

                stopReadings()

                val lastToken = tokens.lastOrNull()
                val currentOperator = operatorStr.toOperator()
                when {
                    symbol == '-' && (tokens.isEmpty() || lastToken is Operator || lastToken == LeftBracket || lastToken == Assign || lastToken == From || lastToken == To || lastToken == Step || lastToken == BlockStart || lastToken == BlockEnd) -> tokens.add(
                        UnaryMinus
                    )

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