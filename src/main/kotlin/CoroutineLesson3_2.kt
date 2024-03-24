import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

//没有真正挂起的suspendCoroutine
suspend fun main() {
    println(notSuspend())
}

suspend fun notSuspend() = suspendCoroutine<Int> {  continuation ->
    continuation.resume(100)
}