package com.woocommerce.android.benchmarkable

/**
 * Some trivial sorting algorithms for benchmarks
 */
object SortingAlgorithms {

    fun bubbleSort(array: IntArray) {
        for (i in 0..array.lastIndex) {
            for (j in 0 until array.lastIndex) {
                if (array[j] > array[j + 1]) {
                    swap(array, j, j + 1)
                }
            }
        }
    }

    fun quickSort(array: IntArray, begin: Int = 0, end: Int = array.lastIndex) {
        if (end <= begin) return
        val pivot = partition(array, begin, end)

        quickSort(array, begin, pivot - 1)
        quickSort(array, pivot + 1, end)
    }

    private fun partition(array: IntArray, begin: Int, end: Int): Int {
        var counter = begin
        for (i in begin until end) {
            if (array[i] < array[end]) {
                swap(array, counter, i)
                counter++
            }
        }
        swap(array, end, counter)
        return counter
    }

    private fun swap(arr: IntArray, i: Int, j: Int) {
        val temp = arr[i]
        arr[i] = arr[j]
        arr[j] = temp
    }
}


/**
 * Check if the array is already sorted
 */
val IntArray.isSorted
    get() = this
        .asSequence()
        .zipWithNext { a, b -> a <= b }
        .all { it }

