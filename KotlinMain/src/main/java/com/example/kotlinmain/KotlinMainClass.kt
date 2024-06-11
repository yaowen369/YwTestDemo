package com.example.kotlinmain

import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KotlinMainClass {
    fun fun1(content:String){
        println( "$content,123")

    }



}

fun main(args: Array<String>) {
    val result = KotlinMainClass().fun1("123")
}