package com.github.fevernova.h;


import com.github.fevernova.framework.common.Util;


public class Start {


    public static void main(String[] args) {

        Election job1 = new Election("oracle", "1", "redis://127.0.0.1:6379");
        Election job2 = new Election("oracle", "2", "redis://127.0.0.1:6379");
        new Thread(job1).start();
        new Thread(job2).start();
        Util.sleepSec(3600);
    }

}
