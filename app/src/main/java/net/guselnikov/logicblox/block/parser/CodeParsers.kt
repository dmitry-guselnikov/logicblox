package net.guselnikov.logicblox.block.parser


/**
 * Нужно создать активити, которая будет навигировать в EditCode или в EditBlock
 *
 * А вот только затем заниматься парсингом кода
 * Сделать автоматическую подстановку нужного количества пробелов в зависимости от вложенности
 */
sealed class ExpressionToken
//data object LeftBracket: ExpressionToken()
//data object RightBracket: ExpressionToken()
//data object LeftCurlyBrace: ExpressionToken()
//data object RightCurlyBrace: ExpressionToken()
//data object EndOfLine: ExpressionToken()

sealed class Expression
class FormulaExpression(tokens: List<ExpressionToken>) : Expression()
class ConditionExpression(
    condition: List<ExpressionToken>,
    onTrueBlock: List<Expression>,
    onFalseBlock: List<Expression>
) : Expression()

/**
 * Общие мысли: если встретили символы '//', то игнорируем всё до конца строки
 *
 * 1. Разбить code на строки
 * 2. Разбить строку на токены
 * 3. Если среди токенов нет условия И если последний токен не является бинарным оператором, то парсим FormulaExpression
 * 4. Если последний токен является бинарным оператором, конкатенируем текущую строку со следующей, идем в пункт 3
 * 5. Если есть условие, то считываем сначала условие, потом TRUE-блок (см. комментарии к parseBlock)
 * 6. Если есть else, то считываем FALSE-блок
 * 6. На вход методу parseBlock подаётся подстрока после условия или после слова else
 */
fun parseCode(code: String): List<Expression> {
    code.split("\n")

    TODO()
}

/**
 * На вход подаётся подстрока кода, например в условном операторе после части Condition
 *
 * Вводные: блок может начинаться с символа {. Если он начинается с {, то обязан закончиться символом }
 * Если фигурной скобки нет, то блок может содержать всего лишь один Expression
 * Если фигурная скобка есть, то считываем блок до символа }, сохраняя чётность фигурных скобок
 *
 * Примерный алгоритм (псевдокод) считывания блока до «правильной» закрывающей фигурной скобки:
 * - nesting = 1
 * for all chars {
 *     if (chars == '{') nesting++
 *     if (char == '}') nesting--
 *     block.append(char)
 *     if (nesting == 0) return
 * }
 *
 * if (nesting != 0) throw (Невалидное количество скобочек)
 *
 * blockExpressions = parseCode(block)
 *
 * Примерный алгоритм чтения блока без фигурной скобки:
 */
fun parseBlock(code: String): List<Expression> {
    TODO()
}

/**
 * Вводные: code - подстрока кода, его не надо парсить до конца, парсим только до «правильной»
 * закрывающей скобки
 *
 * val read = false
 * val nesting = 0
 * val stringBuilder
 *
 * while (nesting != 0 && !read) {
 *     if (char == '(') nesting++
 *     if (char == ')') nesting--
 *     if (nesting < 0) throw ("Хорошее объяснение что конкретно наебнулось — пригодится")
 *     stringBuilder.append(char)
 * }
 */
fun parseCondition(code: String): FormulaExpression {
    TODO()
}