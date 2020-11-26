package com.github.fevernova.task.markettracing.data.order;


public enum OrderType {

    RETRACEMENT(0),
    REBOUND(1),
    DOWN(2),
    UP(3);

    public byte code;


    OrderType(int code) {

        this.code = (byte) code;
    }


    public static OrderType of(int code) {

        switch (code) {
            case 0:
                return RETRACEMENT;
            case 1:
                return REBOUND;
            case 2:
                return DOWN;
            case 3:
                return UP;
            default:
                throw new IllegalArgumentException("unknown OrderType:" + code);
        }
    }
}
