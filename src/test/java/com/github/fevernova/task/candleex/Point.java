package com.github.fevernova.task.candleex;


import com.google.common.collect.Lists;

import java.util.List;


public class Point {


    private long timeSequence;

    private long open;

    private Long high;

    private int highIndex;

    private Long low;

    private int lowIndex;

    private long close;

    private int index;


    public Point(long timeSequence) {

        this.timeSequence = timeSequence;
    }


    public void input(long price) {

        if (this.index == 0) {
            this.open = price;
        } else if (this.index > 1) {
            if (this.high == null || this.high < this.close) {
                this.high = this.close;
                this.highIndex = this.index - 1;
            }
            if (this.low == null || this.low > this.close) {
                this.low = this.close;
                this.lowIndex = this.index - 1;
            }
        }
        this.close = price;
        this.index++;
    }


    public List<Long> result() {

        if (this.index == 0) {
            return null;
        }

        if (this.index == 1) {
            return Lists.newArrayList(this.open);
        }

        if (this.index == 2) {
            if (this.open == this.close) {
                return Lists.newArrayList(this.open);
            } else {
                return Lists.newArrayList(this.open, this.close);
            }
        }

        long p1 = this.open;
        long p2 = this.highIndex < this.lowIndex ? this.high : this.low;
        long p3 = this.highIndex < this.lowIndex ? this.low : this.high;
        long p4 = this.close;

        List<Long> result = Lists.newArrayList(p1);
        push(result, p4, p3, push(result, p3, p2, push(result, p2, p1, 0)));
        return result;
    }


    private int push(List<Long> r, long current, long last, int direction) {

        int newDirection = Long.compare(current, last);
        if (newDirection > 0) {
            if (direction > 0) {
                r.set(r.size() - 1, current);
            } else {
                r.add(current);
            }
        } else if (newDirection < 0) {
            if (direction < 0) {
                r.set(r.size() - 1, current);
            } else {
                r.add(current);
            }
        }
        return newDirection;
    }
}
