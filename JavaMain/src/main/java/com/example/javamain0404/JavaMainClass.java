package com.example.javamain0404;

import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class JavaMainClass {
    public static void main(String[] args) {
        System.out.println("11111");

        for (String str : args) {
            System.out.println(str);
        }

        System.out.println("22222");
//
//        Scanner scanner = new Scanner(System.in);
//        while (scanner.hasNext()) {
//            System.out.println("next输出:" + scanner.next());
//        }
//
//        System.out.println("33333");
//
//        while (scanner.hasNextInt()) {
//            System.out.println("Int输出:" + scanner.nextInt());
//        }
//
//        while (scanner.hasNextInt()) {
//            System.out.println("Line输出:" + scanner.nextLine());
//        }
    }
}