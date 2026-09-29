import kotlinx.coroutines.flow.*

fun main() {
    val a = MutableStateFlow(false)
    val b = MutableStateFlow<Int?>(null)
    
    val c = combine(a, b) { aa, bb -> Pair(aa, bb) }
        .flatMapLatest { (aa, bb) ->
            if (aa) {
                flowOf(listOf("Incognito"))
            } else {
                flowOf(listOf("Normal"))
            }
        }
        
    println(c)
}
