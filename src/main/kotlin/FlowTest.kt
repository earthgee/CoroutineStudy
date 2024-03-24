import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//kotlin破解协程 flow
suspend fun main() {
//    simpleFlow1()

//    triggerSimpleFlow()

//    testFlowConflate()

//    testSharedFlow1()

//    testSharedFlow2()

    testSharedFlow3()
}

//简单flow
suspend fun simpleFlow1() {
    val intFlow = flow<Int> {
        (1..3).forEach {
            emit(it)
            log("flow emit $it")
            delay(100)
        }
    }

    //flowOn 运行时绑定调度器
    GlobalScope.launch {
        intFlow.flowOn(Dispatchers.IO)
            .catch { _ ->
                emit(-1)
            }.collect {
                log("flow collect $it")
            }
    }.join()
}

//flow消费和触发分离
fun createSimpleFlow2() = flow<Int> {
    (1..3).forEach {
        emit(it)
        delay(100)
    }
}.onEach { log("flow collect $it") }

suspend fun triggerSimpleFlow() {
//    GlobalScope.launch {
//        createSimpleFlow2().collect()
//    }.join()

    createSimpleFlow2().launchIn(GlobalScope).join()
}

//flow的背压
suspend fun testFlowConflate() {
    GlobalScope.launch {
        flow<Int> {
            List(100) {
                emit(it)
            }
        }.conflate()
            .collect {
               delay(100)
                log("flow collect $it")
            }
    }.join()
}

//sharedFlow 1:生产者先发送数据
//结论，无法收到数据
fun testSharedFlow1() {
    runBlocking {
        val flow = MutableSharedFlow<String>()
        flow.emit("hello world")

        GlobalScope.launch {
            flow.collect {
                println("collect: $it")
            }
        }
    }
}

//sharedFlow 2:消费者先消费数据
//结论，可以收到数据
fun testSharedFlow2() {
    runBlocking {
        val flow = MutableSharedFlow<String>()
        GlobalScope.launch {
            flow.collect {
                println("collect: $it")
            }
        }
        delay(200)
        flow.emit("hello world")
    }
}

//sharedFlow 3:历史状态的重放
//与2效果相同
fun testSharedFlow3() {
    runBlocking {
        val flow = MutableSharedFlow<String>(1)
        flow.emit("hello world")
        GlobalScope.launch {
            flow.collect {
                println("collect: $it")
            }
        }
    }
}


