import kotlinx.coroutines.delay
import kotlin.coroutines.*

interface AsyncScope {

    suspend fun <T> await(block: () -> T) = suspendCoroutine<T> { continuation ->
        try {
            continuation.resume(block())
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

}

fun async(context: CoroutineContext = EmptyCoroutineContext,
          block: suspend AsyncScope.() -> Unit) {
    val complete = AsyncContinuation(context)
    block.startCoroutine(complete, complete)
}

class AsyncContinuation(override val context: CoroutineContext)
    : Continuation<Unit>, AsyncScope {

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
    }

}

fun main() {
    async {
        val result = await {
            "hello"
        }
        println(result)
    }
}