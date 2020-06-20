package com.github.fevernova.task.candleex;


import com.github.fevernova.framework.common.Util;
import org.junit.Test;

import java.util.Arrays;


public class T_Point {


    @Test
    public void T_point() {

        long[] prices0 = {5, 5, 5, 5, 5};
        test(prices0);
        long[] prices1 = {5, 2, 4, 2, 1};
        test(prices1);
        long[] prices2 = {1, 2, 3, 4, 5};
        test(prices2);
        long[] prices3 = {3, 1, 5, 2, 4};
        test(prices3);
        long[] prices4 = {5, 1, 4, 2, 3};
        test(prices4);
        long[] prices5 = {1, 5, 2, 4, 3};
        test(prices5);
        long[] prices6 = {3, 5, 1, 5, 1, 5, 1, 3};
        test(prices6);
    }


    public void test(long[] prices) {

        Point point = new Point(Util.nowSec());
        for (long price : prices) {
            point.input(price);
        }
        System.out.println("source : " + Arrays.toString(prices));
        System.out.println("result : " + point.result());
    }
}
