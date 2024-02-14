import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.random.Random

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
        val disposable = CancellationHandlerDisposable(this, onCancel)
        val newState = state.updateAndGet {  prev ->
            when(prev) {
                is CoroutineState.Incomplete -> {
                    CoroutineState.Incomplete().from(prev).with(disposable)
                }
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> {
                    prev
                }
            }
        }

        (newState as? CoroutineState.Cancelling)?.let {
            onCancel()
        }
        return disposable
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
        val prevState = state.getAndUpdate { prev ->
            when(prev) {
                is CoroutineState.Incomplete -> {
                    CoroutineState.Cancelling()
                }
                is CoroutineState.Cancelling,
                is CoroutineState.Complete<*> -> prev
            }
        }

        if(prevState is CoroutineState.Incomplete) {
            prevState.notifyCancellation()
            prevState.clear()
        }
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
            is CoroutineState.Complete<*> -> {
                val currentCallingJobState = coroutineContext[CJob]?.isActive ?: return
                if(!currentCallingJobState) {
                    throw CancellationException("Coroutine is Cancelled")
                }
                return
            }
        }
    }

    private suspend fun joinSuspend() =
        csuspendCancellableCoroutine<Unit> { continuation ->
            val disposable = doOnCompleted {  result ->
                continuation.resume(Unit)
            }

            continuation.invokeOnCancellation {
                disposable.dispose()
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

        when(newState) {
            is CoroutineState.Complete<*> -> makeCompletion(newState as CoroutineState.Complete<T>)
        }

    }

    protected open fun handleJobException(e: Throwable) = false

    private fun makeCompletion(newState: CoroutineState.Complete<T>) {
        val result = if(newState.exception == null) {
            Result.success(newState.value)
        } else {
            Result.failure<T>(newState.exception)
        }

        result.exceptionOrNull()?.let(::tryHandleException)

        newState.notifyCompletion(result)
        newState.clear()
    }

    private fun tryHandleException(e: Throwable): Boolean {
        return when(e) {
            is CancellationException -> false
            else -> {
                handleJobException(e)
            }
        }
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

class CancellationHandlerDisposable(val job: CJob, val onCancel: OnCancel): Disposable {

    override fun dispose() {
        job.remove(this)
    }

}

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

    fun notifyCancellation() {
        this.disposableList.loopOn<CancellationHandlerDisposable> {
            it.onCancel()
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

class StandaloneCoroutine(context: CoroutineContext): AbstractCoroutine<Unit>(context) {

    override fun handleJobException(e: Throwable): Boolean {
        super.handleJobException(e)
        context[CoroutineExceptionHandler]?.handleException(context, e)
            ?: Thread.currentThread().let {
                it.uncaughtExceptionHandler.uncaughtException(it, e)
            }
        return true
    }

}

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
    val completion = StandaloneCoroutine(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}

fun <T> casync(context: CoroutineContext = EmptyCoroutineContext,
           block: suspend () -> T): CDeferred<T> {
    val completion = CDeferredCoroutine<T>(newCoroutineContext(context))
    block.startCoroutine(completion)
    return completion
}

interface CDispatcher {
    fun dispatch(block: () -> Unit)
}

open class DispatcherContext(private val dispatcher: CDispatcher)
    : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {

        override fun <T> interceptContinuation(continuation: Continuation<T>)
                : Continuation<T> = DispatchedContinuation(continuation, dispatcher)

}

private class DispatchedContinuation<T>(val delegate: Continuation<T>,
                                        val dispatcher: CDispatcher): Continuation<T> {

    override val context: CoroutineContext = delegate.context

    override fun resumeWith(result: Result<T>) {
        dispatcher.dispatch {
            delegate.resumeWith(result)
        }
    }

}

object DefaultDispatcher: CDispatcher {

    private val threadGroup = ThreadGroup("DefaultDispatcher")
    private val threadIndex = AtomicInteger(0)

    private val executor = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors() +1
    ) { runnable ->
        Thread(threadGroup, runnable,
            "${threadGroup.name}=worker-${threadIndex.getAndIncrement()}").apply {
                isDaemon = true
        }
    }

    override fun dispatch(block: () -> Unit) {
        executor.submit(block)
    }


}

object CDispatchers {
    val Default by lazy {
        DispatcherContext(DefaultDispatcher)
    }
}

fun newCoroutineContext(context: CoroutineContext): CoroutineContext {
    val combined = context +
            CCoroutineName("@coroutine")
    return if(combined != CDispatchers.Default
        && combined[ContinuationInterceptor] == null) {
        combined + CDispatchers.Default
    } else {
        combined
    }
}

class CCoroutineName(val name: String): CoroutineContext.Element {

    companion object Key: CoroutineContext.Key<CCoroutineName>

    override val key = Key

    override fun toString() = name

}

//cancel
suspend inline fun <T> csuspendCancellableCoroutine(
    crossinline block: (CancellableContinuation<T>) -> Unit): T =
    suspendCoroutineUninterceptedOrReturn { continuation ->
        val cancellable = CancellableContinuation(continuation.intercepted())
        block(cancellable)
        cancellable.getResult()
    }

sealed class CancelState {
    object InComplete: CancelState()
    class CancelHandler(val onCancel: OnCancel): CancelState()
    class Complete<T>(val value: T? = null,
                      val exception: Throwable?? = null): CancelState()
    object Cancelled: CancelState()
}

enum class CancelDecision {
    UNDECIDED, SUSPENDED, RESUMED
}

class CancellableContinuation<T>(private val continuation: Continuation<T>)
    : Continuation<T> by continuation {

    private val state = AtomicReference<CancelState>(CancelState.InComplete)
    private val decision = AtomicReference(CancelDecision.UNDECIDED)

    val isCompleted: Boolean
        get() = when(state.get()){
            CancelState.InComplete,
            is CancelState.CancelHandler -> false
            is CancelState.Complete<*>,
            CancelState.Cancelled -> true
        }

    fun invokeOnCancellation(onCancel: OnCancel) {
        val newState = state.updateAndGet {  prev ->
            when(prev) {
                CancelState.InComplete -> CancelState.CancelHandler(onCancel)
                is CancelState.CancelHandler -> throw IllegalStateException("prohibit")
                is CancelState.Complete<*>,
                CancelState.Cancelled -> prev
            }
        }
        if(newState is CancelState.Cancelled) {
            onCancel()
        }
    }

    fun cancel() {
        if(isCompleted) {
            return
        }

        val parent = continuation.context[CJob] ?: return
        parent.cancel()
    }

    private fun installCancelHandler() {
        if(isCompleted) {
            return
        }
        val parent = continuation.context[CJob] ?: return
        parent.invokeOnCancel {
            doCancel()
        }
    }

    private fun doCancel() {
        val prevState = state.getAndUpdate {  prev ->
            when(prev) {
                is CancelState.CancelHandler,
                CancelState.InComplete -> {
                    CancelState.Cancelled
                }
                CancelState.Cancelled,
                is CancelState.Complete<*> -> {
                    prev
                }
            }
        }

        if(prevState is CancelState.CancelHandler) {
            prevState.onCancel()
            resumeWithException(CancellationException("Cancelled"))
        }
    }

    fun getResult(): Any? {
        installCancelHandler()
        if(decision.compareAndSet(CancelDecision.UNDECIDED, CancelDecision.SUSPENDED)) {
            return COROUTINE_SUSPENDED
        }

        return when(val currentState = state.get()) {
            is CancelState.CancelHandler,
            CancelState.InComplete -> {
                COROUTINE_SUSPENDED
            }
            CancelState.Cancelled ->
                throw CancellationException("Continuation is cancel")
            is CancelState.Complete<*> -> {
                (currentState as CancelState.Complete<T>).let {
                    it.exception?.let { throw it } ?: it.value
                }
            }
        }
    }

    override fun resumeWith(result: Result<T>) {
        when {
            decision.compareAndSet(CancelDecision.UNDECIDED, CancelDecision.RESUMED) -> {
                state.set(CancelState.Complete(result.getOrNull(), result.exceptionOrNull()))
            }
            decision.compareAndSet(CancelDecision.SUSPENDED, CancelDecision.RESUMED) -> {
                state.updateAndGet {  prev ->
                    when(prev) {
                        is CancelState.Complete<*> -> {
                            throw IllegalStateException("Already completed")
                        }
                        else -> {
                            CancelState.Complete(result.getOrNull(), result.exceptionOrNull())
                        }
                    }
                }
            }
        }
        continuation.resumeWith(result)
    }

}

//exception
interface CoroutineExceptionHandler: CoroutineContext.Element {

    companion object Key: CoroutineContext.Key<CoroutineExceptionHandler>

    fun handleException(context: CoroutineContext, exception: Throwable)

}

inline fun CoroutineExceptionHandler(crossinline handler: (CoroutineContext, Throwable) -> Unit)
    : CoroutineExceptionHandler =
        object: AbstractCoroutineContextElement(CoroutineExceptionHandler), CoroutineExceptionHandler {

            override fun handleException(context: CoroutineContext, exception: Throwable) {
                handler.invoke(context, exception)
            }

        }

suspend fun main() {
//    testCLaunch()

//    testAsync()

//    testThreadAsync()

//    testCancel()

    testException()
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
        println("casync thread:${Thread.currentThread().name}")
        delay(1000L)
        "Hello World"
    }
    println("await thread:${Thread.currentThread().name}")
    println(deferred.await())
}

suspend fun testThreadAsync() {
    val deferred = casync {
        println("casync thread:${Thread.currentThread().name}")
        delay(1000L)
        println("casync thread:${Thread.currentThread().name}")
        "Hello World"
    }
    println("await thread:${Thread.currentThread().name}")
    println(deferred.await())
}

private suspend fun testCancel() {
    val job = claunch {
        println("testCancel")
        val r0 = nonCancellableFunction()
        println("r0:$r0")
        val r1 = cancellableFunction()
        println("r1:$r1")
    }
    job.invokeOnCancel {
        println("job onCancel")
    }

    job.cancel()
    job.join()
}

suspend fun nonCancellableFunction() = suspendCoroutine<Int> { continuation ->

    val completableFuture = CompletableFuture.supplyAsync {
        Thread.sleep(1000L)
        Random.nextInt()
    }

    completableFuture.thenApply {
        continuation.resume(it)
    }.exceptionally {
        continuation.resumeWithException(it)
    }

}

suspend fun cancellableFunction() = csuspendCancellableCoroutine<Int> { continuation ->

    val completableFuture = CompletableFuture.supplyAsync {
        Thread.sleep(1000L)
        Random.nextInt()
    }

    continuation.invokeOnCancellation {
        println("async task cancel")
        completableFuture.cancel(true)
    }

    completableFuture.thenApply {
        continuation.resume(it)
    }.exceptionally {
        continuation.resumeWithException(it)
    }

}

suspend fun testException() {
    val exceptionHandler = CoroutineExceptionHandler {
        coroutineContext, throwable ->
        println(throwable.message)
    }

    val job = claunch(exceptionHandler) {
        println("hello")
        throw NullPointerException("aha null")
        println("world")
    }
    job.invokeOnCompletion {
        println("job onComplete")
    }
    job.join()
}

