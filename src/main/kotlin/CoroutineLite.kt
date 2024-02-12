import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

interface CJob: CoroutineContext.Element {

    companion object Key: CoroutineContext.Key<CJob>

    override val key: CoroutineContext.Key<*>
        get() = CJob

    val isActive: Boolean

    fun invokeOnCancel(onCancel: OnCancel): Disposable

    fun invokeOnCompletion(onComplete: OnComplete): Disposable

    fun cancel()

    fun remove(disposable: Disposable)

    suspend fun join()

}

interface CDeferred<T>: CJob {

    suspend fun await(): T

}

abstract class AbstractCoroutine<T>(context: CoroutineContext)
    : CJob, Continuation<T> {

    protected val state = AtomicReference<CoroutineState>()

    override val context: CoroutineContext

    val isCompleted
        get() = state.get() is CoroutineState.Complete<*>

    override val isActive: Boolean
        get() = when(state.get()) {
            is CoroutineState.Complete<*>,
            is CoroutineState.Cancelling -> false
            else -> true
        }

    init {
        state.set(CoroutineState.Incomplete())
        this.context = context + this
    }

    override fun invokeOnCancel(onCancel: OnCancel): Disposable {
        TODO("Not yet implemented")
    }

    override fun invokeOnCompletion(onComplete: OnComplete): Disposable {
        return doOnCompleted { _ ->
            onComplete()
        }
    }

    protected fun doOnCompleted(block: (Result<T>) -> Unit): Disposable {
        val disposable = CompletionHandlerDisposable(this, block)
        val newState = state.updateAndGet {  prev ->
            when(prev) {
                is CoroutineState.Incomplete ->
                    CoroutineState.Incomplete().from(prev).with(disposable)
                is CoroutineState.Cancelling ->
                    CoroutineState.Cancelling().from(prev).with(disposable)
                is CoroutineState.Complete<*> -> prev
            }
        }

        (newState as? CoroutineState.Complete<T>)?.let {
            block(
                when {
                    it.value != null -> Result.success(it.value)
                    it.exception != null -> Result.failure(it.exception)
                    else -> throw IllegalStateException("no way")
                }
            )
        }

        return disposable
    }

    override fun cancel() {
        //TODO
    }

    override fun remove(disposable: Disposable) {
        val newState = state.updateAndGet {  prev ->
            when(prev) {
                is CoroutineState.Incomplete ->
                    CoroutineState.Incomplete().from(prev).without(disposable)
                is CoroutineState.Cancelling ->
                    CoroutineState.Cancelling().from(prev).without(disposable)
                is CoroutineState.Complete<*> ->
                    prev
            }
        }
    }

    override suspend fun join() {
        when(state.get()) {
            is CoroutineState.Incomplete,
            is CoroutineState.Cancelling ->
                return joinSuspend()
            is CoroutineState.Complete<*> ->
                return
        }
    }

    private suspend fun joinSuspend() =
        suspendCoroutine<Unit> { continuation ->
        doOnCompleted {  result ->
            continuation.resume(Unit)
        }
    }

    override fun resumeWith(result: Result<T>) {
        val newState = state.updateAndGet {  prev ->
            when(prev) {
                is CoroutineState.Cancelling,
                is CoroutineState.Incomplete -> {
                    CoroutineState.Complete(result.getOrNull()
                        , result.exceptionOrNull()).from(prev)
                }
                is CoroutineState.Complete<*> -> {
                    throw IllegalStateException("Already completed")
                }
            }
        }

        newState.notifyCompletion(result)
        newState.clear()
    }

}

interface Disposable {

    fun dispose()

}

class CompletionHandlerDisposable<T>(
    val job: CJob,
    val onComplete: (Result<T>) -> Unit) : Disposable {
    override fun dispose() {
        job.remove(this)
    }

}

//class CancellationHandlerDisposable<T>

typealias OnCancel = () -> Unit

typealias OnComplete = () -> Unit

sealed class CoroutineState {
    class Incomplete: CoroutineState()
    class Cancelling: CoroutineState()
    class Complete<T>(val value: T? = null, val exception: Throwable? = null)
        : CoroutineState()

    private var disposableList: DisposableList = DisposableList.Nil()

    fun from(state: CoroutineState): CoroutineState {
        this.disposableList = state.disposableList
        return this
    }

    fun with(disposable: Disposable): CoroutineState {
        this.disposableList = DisposableList.Cons(disposable, this.disposableList)
        return this
    }

    fun without(disposable: Disposable): CoroutineState {
        this.disposableList = this.disposableList.remove(disposable)
        return this
    }

    fun clear() {
        this.disposableList = DisposableList.Nil()
    }

    fun <T> notifyCompletion(result: Result<T>) {
        this.disposableList.loopOn<CompletionHandlerDisposable<T>> {
            it.onComplete(result)
        }
    }

}

sealed class DisposableList {
    class Nil: DisposableList()
    class Cons(val head: Disposable, val tail: DisposableList): DisposableList()

}

fun DisposableList.remove(disposable: Disposable): DisposableList {
    return when(this) {
        is DisposableList.Nil -> return this
        is DisposableList.Cons -> {
            if(head == disposable) {
                return tail
            } else {
                DisposableList.Cons(head, tail.remove(disposable))
            }
        }
    }
}

tailrec fun DisposableList.forEach(action: (Disposable)-> Unit): Unit {
    when(this) {
        is DisposableList.Nil -> Unit
        is DisposableList.Cons -> {
            action(head)
            tail.forEach(action)
        }
    }
}

inline fun <reified T: Disposable> DisposableList.loopOn(crossinline action: (T) -> Unit) =
    forEach {
        if(it is T) {
            action(it)
        }
    }

class StandaloneCoroutine(context: CoroutineContext): AbstractCoroutine<Unit>(context)

class CDeferredCoroutine<T>(context: CoroutineContext)
    : AbstractCoroutine<T>(context), CDeferred<T> {
    override suspend fun await(): T {
        val currentState = state.get()
        return when(currentState) {
            is CoroutineState.Incomplete,
            is CoroutineState.Cancelling -> awaitSuspend()
            is CoroutineState.Complete<*> -> {
                currentState.exception?.let {
                    throw it
                } ?:  (currentState.value as T)
            }
        }
    }

    private suspend fun awaitSuspend() =
        suspendCoroutine<T> {  continuation ->
        doOnCompleted {  result ->
            continuation.resumeWith(result)
        }
    }

}

fun claunch(context: CoroutineContext = EmptyCoroutineContext,
           block: suspend () -> Unit): CJob {
    val completion = StandaloneCoroutine(context)
    block.startCoroutine(completion)
    return completion
}

fun <T> casync(context: CoroutineContext = EmptyCoroutineContext,
           block: suspend () -> T): CDeferred<T> {
    val completion = CDeferredCoroutine<T>(context)
    block.startCoroutine(completion)
    return completion
}

suspend fun main() {
    testAsync()
}

suspend fun testCLaunch() {
    val job = claunch {
        println("hello")
        delay(1000L)
        println("world")
    }
    job.invokeOnCompletion {
        println("job onComplete")
    }
    job.join()
}

suspend fun testAsync() {
    val deferred = casync {
        delay(1000L)
        "Hello World"
    }
    println(deferred.await())
}



