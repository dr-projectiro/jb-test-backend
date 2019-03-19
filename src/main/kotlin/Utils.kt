// gets three random items from the list
fun <T> List<T>.threeRandoms(): List<T> =
    if (size <= 3) {
        this
    } else {
        val first = random()
        val second = minus(first).random()
        val third = minus(listOf(first, second)).random()
        listOf(first, second, third)
    }
