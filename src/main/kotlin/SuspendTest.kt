import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.reflect.KMutableProperty0

class EatGame {
    private var feedContinuation: Continuation<Int>? = null
    private var eatContinuation: Continuation<String>? = null
    private var eatAttempts = 0

    var isActive: Boolean = true
        private set

    suspend fun eat(): String {
        return if(isActive) {
            suspendCoroutine {
                this.eatContinuation = it
                resumeContinuation(this::feedContinuation, eatAttempts++)
            }
        } else ""
    }

    suspend fun feed(food: String): Int {
        return if(isActive) {
            suspendCoroutine {
                this.feedContinuation = it
                resumeContinuation(this::eatContinuation, food)
            }
        } else -1
    }

    fun end() {
        isActive = false
        resumeContinuation(this::feedContinuation, -1)
        resumeContinuation(this::eatContinuation, "")
    }

    private fun <T> resumeContinuation(
        continuationRef: KMutableProperty0<Continuation<T>?>, value: T) {
        val continuation = continuationRef.get()
        continuationRef.set(null)
        continuation?.resume(value)
    }

}

suspend fun eatGame() {
    coroutineScope {
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val game = EatGame()
        launch(dispatcher) {
            println("Start")
            delay(1000L)
            game.end()
            println("End")
        }
        launch(dispatcher) {
            while(game.isActive) {
                delay(60L)
                val food = Math.random()
                println("#1 feed food $food")
                println("#1 Complete: ${game.feed("$food")}")
            }
        }
        launch(dispatcher) {
            while(game.isActive) {
                delay(50L)
                println("#2 Eat ${game.eat()}")
            }
        }
    }
}

suspend fun main() {
    eatGame()
}