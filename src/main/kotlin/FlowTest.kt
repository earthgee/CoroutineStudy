import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

//kotlin破解协程 flow
suspend fun main() {
//    simpleFlow1()

//    triggerSimpleFlow()

//    testFlowZip()

//    testFlowMerge()

//    testFlowCombine()

//    testFlowConflate()

//    testSharedFlow1()

//    testSharedFlow2()

//    testSharedFlow3()

    testSharedFlowTransform()

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
//        intFlow.transform {
//            emit(it)
//            emit("transform $it")
//        }.collect {
//            println(it)
//        }

        intFlow.filter {
            it > 0
        }.flowOn(Dispatchers.IO)
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

//多路流处理示例
suspend fun testFlowZip() {
    GlobalScope.launch {
        val flow1 = flowOf(1,2,3)
        val flow2 = flowOf("a", "b", "c")

        flow1.zip(flow2) { num, str ->
            "$num$str"
        }.collect {
            println("zip:$it")
        }
    }.join()
}

suspend fun testFlowMerge() {
    GlobalScope.launch {
        val flow1 = flowOf("d", "e", "f")
        val flow2 = flowOf("a", "b", "c")

        merge(flow1, flow2).collect {
            println("merge: $it")
        }

    }.join()
}

suspend fun testFlowCombine() {
    GlobalScope.launch {
        val flow1 = flowOf(1,2,3)
        val flow2 = flowOf("a", "b", "c")

        flow1.combine(flow2) { num, str ->
            "$num$str"
        }.collect {
            println("combine:$it")
        }
    }.join()
}

suspend fun testSharedFlowTransform() {
    GlobalScope.launch {
        val flow1 = mutableListOf(1,2,3,4).asFlow()
            .shareIn(this, SharingStarted.Eagerly, replay = 4)
        val flow2 = arrayOf(5,6,7,8).asFlow()
            .shareIn(this, SharingStarted.Eagerly, 4)
        val flow3 = MutableStateFlow(9)
        flow3.emit(10)

        launch {
            flow1.collect {
                println("flow1,collect $it")
            }
        }

        launch {
            flow2.collect {
                println("flow2,collect $it")
            }
        }

        launch {
            flow3.collect {
                println("flow3,collect $it")
            }
        }
    }.join()
}






