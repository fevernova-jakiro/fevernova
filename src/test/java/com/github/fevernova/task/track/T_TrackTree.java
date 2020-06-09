package com.github.fevernova.task.track;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;


public class T_TrackTree {


    private TrackTree trackTree;


    @Before
    public void init() {

        this.trackTree = new TrackTree();
        List<Long> result = this.trackTree.newPrice(100L);
        Assert.assertTrue(result.isEmpty());
        Assert.assertEquals(100L, this.trackTree.getLastPrice());
    }


    @Test
    public void T_test() {

        this.trackTree.clear();
        this.trackTree.newPrice(100L);
        this.trackTree.addOrder(1, 5);
        this.trackTree.addOrder(2, 10);
        this.trackTree.addOrder(3, 25);
        printTree();

        priceMove(95);
        priceMove(120);
        priceMove(100);
        priceMove(98);
        priceMove(96);
        priceMove(95);
    }


    private void priceMove(long price) {

        long from = this.trackTree.getLastPrice();
        long to = price;

        List<Long> r = this.trackTree.newPrice(price);
        System.out.println("From : " + from + " To : " + to);
        System.out.println("result : " + r);
        printTree();
    }


    private void printTree() {

        System.out.println("Tree : " + this.trackTree.getTree());
    }
}
