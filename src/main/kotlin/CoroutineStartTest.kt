import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

private fun testStartCoroutine() {
    val continuation = object: Continuation<String> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resumeWith(result: Result<String>) {
            println("reuslt = ${result.getOrNull()}")
        }

    }

    block.startCoroutine(continuation)
}

private val block = suspend {
    println("Hello")
    delay(1000L)
    println("World")
    "Result"
}

fun main() {
    testStartCoroutine()
    Thread.sleep(2000L)
}