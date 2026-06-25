package ru.andrew.application.ui.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Делает скроллинг более плавным (вязким) за счет уменьшения 
 * скорости прокрутки и инерции (fling) на заданный коэффициент.
 */
@Composable
fun Modifier.smoothScroll(multiplier: Float = 0.8f): Modifier {
    val nestedScrollConnection = remember(multiplier) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Забираем (1 - multiplier) часть скролла в никуда, 
                // оставляя списку только multiplier часть (например, 80%)
                return available * (1f - multiplier)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // То же самое для инерционного скролла
                return available * (1f - multiplier)
            }
        }
    }
    return this.nestedScroll(nestedScrollConnection)
}
