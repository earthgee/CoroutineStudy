import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

sealed class Status {
    class Created(val continuation: Continuation<Unit>):Status()
    class Yielded<P>(val continuation: Continuation<P>):Status()
    class Resumed<R>(val continuation: Continuation<R>):Status()
    object Dead:Status()

}

interface CoroutineScope<P, R> {

    val parameter: P?

    suspend fun yield(value: R): P

}

class Coroutine<P, R>(
    override val context: CoroutineContext = EmptyCoroutineContext,
    private val block: suspend CoroutineScope<P, R>.(P) -> R) : Continuation<R> {

        companion object {
            fun <P, R> create(context: CoroutineContext = EmptyCoroutineContext,
                              block: suspend CoroutineScope<P, R>.(P) -> R)
                : Coroutine<P, R>{
                return Coroutine(context, block)
            }
        }

        private val scope = object: CoroutineScope<P, R> {
            override var parameter: P? = null

            override suspend fun yield(value: R): P
                = suspendCoroutine {  continuation ->
                val previousStatus = status.getAndUpdate {
                    when(it) {
                        is Status.Created ->
                            throw IllegalStateException("Never started")
                        is Status.Yielded<*> ->
                            throw IllegalStateException("Already Yield")
                        is Status.Resumed<*> ->
                            Status.Yielded(continuation)
                        Status.Dead ->
                            throw IllegalStateException("Already dead")
                    }
                }

                (previousStatus as Status.Resumed<R>).continuation.resume(value)
            }

        }

        private val status: AtomicReference<Status>

        val isActive: Boolean
            get() = status.get() != Status.Dead

        init {
            val coroutineBlock: suspend CoroutineScope<P, R>.() -> R  = {
                block(parameter!!)
            }
            val start = coroutineBlock.createCoroutine(scope, this)
            status = AtomicReference(Status.Created(start))
        }

        override fun resumeWith(result: Result<R>) {
            val previousStatus = status.getAndUpdate {
                when(it) {
                    is Status.Created ->
                        throw IllegalStateException("Never Created")
                    is Status.Yielded<*> ->
                        throw IllegalStateException("Already Yield")
                    is Status.Resumed<*> -> {
                        Status.Dead
                    }
                    Status.Dead ->
                        throw IllegalStateException("Already dead")
                }
            }

            (previousStatus as Status.Resumed<R>).continuation.resumeWith(result)
        }

        suspend fun resume(value: P): R = suspendCoroutine {  continuation ->
            val previousStatus = status.getAndUpdate {
                when(it) {
                    is Status.Created -> {
                        scope.parameter = value
                        Status.Resumed(continuation)
                    }
                    is Status.Yielded<*> -> {
                        Status.Resumed(continuation)
                    }
                    is Status.Resumed<*> -> {
                        throw IllegalStateException("Already resumed")
                    }
                    Status.Dead -> {
                        throw IllegalStateException("Already dead")
                    }
                }
            }

            when(previousStatus) {
                is Status.Created ->
                    previousStatus.continuation.resume(Unit)
                is Status.Yielded<*> ->
                    (previousStatus as Status.Yielded<P>).continuation.resume(value)
            }
        }

}

suspend fun main() {
    val producer = Coroutine.create<Unit, Int> {
        for(i in 0..3){
            println("send $i")
            yield(i)
        }
        200
    }

    val consumer = Coroutine.create<Int, Unit> { param ->
        println("start param:$param")
        for(i in 0..3){
            val value = yield(Unit)
            println("receive value:$value")
        }
    }

    while(producer.isActive && consumer.isActive) {
        val result = producer.resume(Unit)
        consumer.resume(result)
    }

}