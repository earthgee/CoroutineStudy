import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.concurrent.thread
import kotlin.coroutines.resume

//suspendCancellableCoroutine 测试
suspend fun returnSuspended() = suspendCancellableCoroutine<String> {
    continuation ->
    thread {
        Thread.sleep(1000)
        continuation.resume("Return suspeneded")
    }
}

suspend fun returnImmediately() = suspendCancellableCoroutine<String> {
    it.resume("Return immediately")
}