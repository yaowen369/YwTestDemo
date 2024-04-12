package com.example.kotlinmain

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KotlinMainClass {
    fun fun1(content:String):String{
        return "$content,123"

    }

    fun fun2() {
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        executor.submit(Runnable {
            println("1-1 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
            Thread.currentThread().name = "第一个ThreadName"
            println("1-2 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
        })

        executor.submit(Runnable {
            println("2-1 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
            Thread.currentThread().name = "第2个ThreadName"
            println("2-2 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
        })

        executor.submit(Runnable {
            println("3-1 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
            Thread.currentThread().name = "第3个ThreadName"
            println("3-2 -> ${Thread.currentThread()}, ${Thread.currentThread().id}")
        })
    }

    lateinit var  myName:String
    lateinit var arr:IntArray
    fun fun3 (){
        myName = "123"

        arr = IntArray(5)
        ::arr.isInitialized 
    }

}

fun main(args: Array<String>) {
    val result = KotlinMainClass().fun2()
}