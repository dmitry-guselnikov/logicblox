package net.guselnikov.logicblox.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import net.guselnikov.logicblox.R

/**
 * 1. fun compute -> suspend fun compute
 * 2. WON'T FIX Добавить скобочки для унарного оператора (не обязательно)
 * 3. DONE Добавить возможность сравнения формулами (для ConditionBlock)
 * 4. Написать блок — цикл
 * 5. Сделать возможность аутпута (пригодится для создания консоли вывода, когда будет гуй)
 * 6. (ГУЙ) Элементы блок-схемы могут находиться в дискретных значениях x и y по сетке (сильно упростится хранение графики)
 * 7. (ГУЙ) Во время драг-н-дропа рисовать полупрозрачный элемент, прикованный к сетке (то, куда встанет)
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.code).setOnClickListener {
            startActivity(Intent(this, EditCodeActivity::class.java))
        }

        findViewById<Button>(R.id.block).setOnClickListener {
            startActivity(Intent(this, EditBlockActivity::class.java))
        }
    }

//    private suspend fun solveQuadraticEquationWithFormula() {
//        val a = ValueInteger(1)
//        val b = ValueInteger(1)
//        val c = ValueInteger(-6)
//        val params = mapOf(
//            Pair(0, a),
//            Pair(1, b),
//            Pair(2, c)
//        )
//
//        val aIsNullChecker = ConditionBlock(
//            condition = FormulaCondition("a==0").also {
//                it[0] = "a"
//            },
//            blockOnTrue = 1,
//            blockOnFalse = 2,
//            inputIndicies = listOf(0)
//        )
//
//        val bIsNullChecker = ConditionBlock(
//            condition = FormulaCondition("b==0").also {
//                it[0] = "b"
//            },
//            blockOnTrue = 6,
//            blockOnFalse = 5,
//            inputIndicies = listOf(1)
//        )
//
//        val discriminantChecker = ConditionBlock(
//            condition = FormulaCondition("D<0").also {
//                it[0] = "D"
//            },
//            blockOnTrue = 6,
//            blockOnFalse = 4,
//            inputIndicies = listOf(2)
//        )
//
//        val noSolutionsBlock = ProxyBlock(mapOf(Pair(0, Undefined)))
//        noSolutionsBlock.isFinal = true
//
//        val linearSolutionBlock = FormulaBlock("-c/b")
//        linearSolutionBlock[1] = "b"
//        linearSolutionBlock[2] = "c"
//        linearSolutionBlock.isFinal = true
//
//        val discriminantBlock = FormulaBlock("a", "b", "b*b-4*a*c")
//        discriminantBlock[0] = "a"
//        discriminantBlock[1] = "b"
//        discriminantBlock[2] = "c"
//
//        val solutionBlock = FormulaBlock("(-b+D^0.5)/(2*a)", "(-b-D^0.5)/(2*a)")
//        solutionBlock[0] = "a"
//        solutionBlock[1] = "b"
//        solutionBlock[2] = "D"
//        solutionBlock.isFinal = true
//
//        val values = LinearBlock(
//            aIsNullChecker, // 0
//            bIsNullChecker, // 1
//            discriminantBlock, // 2
//            discriminantChecker, // 3
//            solutionBlock, // 4
//            linearSolutionBlock, // 5
//            noSolutionsBlock // 6
//        ).compute(params)
//
//        val result = StringBuilder()
//        values.forEach { (key, value) ->
//            if (result.isNotEmpty()) result.append("\n")
//            result.append("value${key} = ")
//            if (value is ValueNumber) {
//                result.append(value.toDouble())
//            } else {
//                result.append("Undefined")
//            }
//        }
//
//        val resStr = result.toString()
//        Log.d("Values", resStr)
//    }

//    private suspend fun solveQuadraticEquation() {
//        val discriminantBlock = getDiscriminantBlock()
//        val solutionBlock = getSolutionBlock()
//
//        val a = ValueInteger(1)
//        val b = ValueInteger(1)
//        val c = ValueInteger(-6)
//
//        val quadraticEquationSolver = GroupBlock(
//            blocks = listOf(discriminantBlock, solutionBlock),
//            relations = listOf(
//                BlockRelation(GroupBlock.PARENT_INDEX, 0, 0, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 0, 1, 1),
//                BlockRelation(GroupBlock.PARENT_INDEX, 0, 2, 2),
//                BlockRelation(GroupBlock.PARENT_INDEX, 1, 0, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 1, 1, 1),
//                BlockRelation(0, 1, 0, 2),
//                BlockRelation(1, GroupBlock.PARENT_INDEX, 0, 0),
//                BlockRelation(1, GroupBlock.PARENT_INDEX, 1, 1)
//            )
//        )
//
//        val values = quadraticEquationSolver.compute(
//            mapOf(
//                Pair(0, a),
//                Pair(1, b),
//                Pair(2, c)
//            )
//        )
//
//        Log.d("Values", values.toString())
//    }

//    private fun getDiscriminantBlock(): Block {
//        val block1 = MultiplicationBlock()
//        val block2 = MultiplicationBlock(ValueInteger(4))
//        val block3 = SubtractionBlock()
//
//        return GroupBlock(
//            blocks = listOf(block1, block2, block3),
//            relations = listOf(
//                BlockRelation(GroupBlock.PARENT_INDEX, 0, 1, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 1, 0, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 1, 2, 1),
//                BlockRelation(0, 2, 0, 0),
//                BlockRelation(1, 2, 0, 1),
//                BlockRelation(2, GroupBlock.PARENT_INDEX, 0, 0)
//            )
//        )
//    }

//    private fun getSolutionBlock(): Block {
//        val squareRootBlock = SquareRootBlock()
//        val negativeNumberBlock = MultiplicationBlock(ValueInteger(-1))
//        val denomitatorMultiplierBlock = MultiplicationBlock(ValueInteger(2))
//        val sumBlock = AdditionBlock()
//        val subtractBlock = SubtractionBlock()
//        val positiveDivisionBlock = DivisionBlock()
//        val negativeDivisionBlock = DivisionBlock()
//
//        return GroupBlock(
//            blocks = listOf(
//                denomitatorMultiplierBlock, negativeNumberBlock, squareRootBlock,
//                sumBlock, subtractBlock, positiveDivisionBlock, negativeDivisionBlock
//            ),
//            relations = listOf(
//                BlockRelation(GroupBlock.PARENT_INDEX, 0, 0, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 1, 1, 0),
//                BlockRelation(GroupBlock.PARENT_INDEX, 2, 2, 0),
//                BlockRelation(1, 3, 0, 0),
//                BlockRelation(2, 3, 0, 1),
//                BlockRelation(1, 4, 0, 0),
//                BlockRelation(2, 4, 0, 1),
//                BlockRelation(3, 5, 0, 0),
//                BlockRelation(0, 5, 0, 1),
//                BlockRelation(4, 6, 0, 0),
//                BlockRelation(0, 6, 0, 1),
//                BlockRelation(5, GroupBlock.PARENT_INDEX, 0, 0),
//                BlockRelation(6, GroupBlock.PARENT_INDEX, 0, 1)
//            )
//        )
//    }
}