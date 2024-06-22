package net.guselnikov.logicblox.presentation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * 1. Написать кастомный FrameLayout, который умеет на бэкграунде рисовать сетку
 * 2. Реализовать определение координат при долгом тапе
 * 3. Реализовать отрисовку стандартных блоков
 * 4. Сделав 2 и 3, реализовать определение позиций анкерных точек дочерних элементов
 * 5. Expose setOnLongTapListener(x, y -> { })
 * 6. Expose setOnLongTapListener(elementId, anchorId -> { })
 * 7.
 */
class BlockCanvas(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
}