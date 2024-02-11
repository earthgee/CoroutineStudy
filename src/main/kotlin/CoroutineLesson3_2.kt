import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun main() {
    println(notSuspend())
}

suspend fun notSuspend() = suspendCoroutine<Int> {  continuation ->
    continuation.resume(100)
}