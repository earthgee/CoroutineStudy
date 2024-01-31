import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun testLaunch() {
    val scope = CoroutineScope(Job())
    scope.launch {
        println("Hello")
        delay(1000L)
        println("World")
    }
}

fun main() {
    testLaunch()
    Thread.sleep(2000L)
}

