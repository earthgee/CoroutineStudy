import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

//coroutineScope 和 supervisor作用域区别
suspend fun main() {
    supervisorScopeFun()
}

private suspend fun coroutineScopeFun() {
    log(1)
    try {
        coroutineScope {
            log(2)
            launch {
                log(3)
                launch {
                    log(4)
                    delay(100)
                    throw IllegalStateException("test")
                }
                log(5)
            }
            log(6)
            val job = launch {
                log(7)
                delay(1000)
            }
            try {
                log(8)
                job.join()
                log(9)
            } catch (ex: Exception) {
                log("10.$ex")
            }
        }
        log(11)
    } catch (ex: Exception) {
        log("12.$ex")
    }
    log(13)
}

private suspend fun supervisorScopeFun() {
    log(1)
    try {
        supervisorScope { //1
            log(2)
            launch {  //2
                log(3)
                launch {  //3
                    log(4)
                    delay(100)
                    throw IllegalStateException("test")
                }
                log(5)
            }
            log(6)
            val job = launch {
                log(7)
                delay(1000)
            }
            try {
                log(8)
                job.join()
                log(9)
            } catch (ex: Exception) {
                log("10.$ex")
            }
        }
        log(11)
    } catch (ex: Exception) {
        log("12.$ex")
    }
    log(13)
}



