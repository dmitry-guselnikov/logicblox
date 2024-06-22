package net.guselnikov.logicblox.block

/*
 * Класс ответственный за блок
 *
 * – Блок имеет набор входов и выходов
 * – Для каждого выхода блок имеет алгоритм вычисления значения
 * – Тип входного и выходного значения – наследник ValueType
 * – Вычисление всего блока происходит вызовом одного метода, в параметрах которого все инпуты,
 *   а возврашаемое значение – массив ValueType
 *
 *
 */
abstract class Block {

    // Флаг для линейных блоков, говорящий о том, что блок финальный и дальше не надо идти
    var isFinal: Boolean = false
    abstract suspend fun compute(inputs: Map<Int, ValueType>): Map<Int, ValueType>
}