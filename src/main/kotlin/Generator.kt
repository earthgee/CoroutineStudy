import java.lang.IllegalStateException
import java.lang.IndexOutOfBoundsException
import kotlin.coroutines.*

interface Generator<T> {
    operator fun iterator(): Iterator<T>
}

class GeneratorImpl<T>(
    val block: suspend GeneratorScope<T>.(T) -> Unit,
    val parameter: T): Generator<T> {
    override fun iterator(): Iterator<T> {
        return GeneratorIterator(block, parameter)
    }

}

fun <T> generator(block: suspend GeneratorScope<T>.(T) -> Unit)
    : (T) -> Generator<T> {
    return { parameter: T ->
        GeneratorImpl(block, parameter)
    }
}

sealed class State {
    class NotReady(val continuation: Continuation<Unit>): State()
    class Ready<T>(val continuation: Continuation<Unit>, val nextValue: T): State()
    object Done: State()
}

class GeneratorIterator<T>(val block: suspend GeneratorScope<T>.(T) -> Unit,
                           val parameter: T):
    GeneratorScope<T>, Iterator<T>, Continuation<Any?>{
    private var state: State

    init {
        val coroutineBlock : suspend GeneratorScope<T>.() -> Unit =
            { block(parameter) }
        val start = coroutineBlock.createCoroutine(this, this)
        state = State.NotReady(start)
    }

    private fun resume() {
        when(val currentState = state) {
            is State.NotReady -> {
                println("continuation=${currentState.continuation.hashCode()}")
                currentState.continuation.resume(Unit)
            }
        }
    }

    override fun hasNext(): Boolean {
        resume()
        return state != State.Done
    }

    override fun next(): T {
        return when(val currentState = state) {
            is State.NotReady -> {
                resume()
                return next()
            }
            is State.Ready<*> -> {
                state = State.NotReady(currentState.continuation)
                return currentState.nextValue as T;
            }
            State.Done -> {
                throw IndexOutOfBoundsException("No value left")
            }
        }
    }
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        state = State.Done
        result.getOrThrow()
    }

    override suspend fun yield(value: T)  =
        suspendCoroutine<Unit> { continuation ->
            state = when(state) {
                is State.NotReady ->
                    State.Ready(continuation, value)
                is State.Ready<*> ->
                    throw IllegalStateException("cannot yield while Ready.")
                State.Done ->
                    throw IllegalStateException("cannot yield while done.")
            }
        }

}

@RestrictsSuspension
interface GeneratorScope<T> {

    suspend fun yield(value: T)

}

fun main() {
    val nums = generator<Int> {  start ->
        for(i in 0..5){
            yield(start + i)
        }
    }

    val generator = nums(10)

    for(i in generator) {
        println(i)
    }
}