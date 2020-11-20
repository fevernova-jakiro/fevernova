package com.github.fevernova.task.markettracing.data.order;


public enum OrderType {

    DOWN(1),
    UP(2),
    REBOUND(3),
    RETRACEMENT(4);

    public byte code;


    OrderType(int code) {

        this.code = (byte) code;
    }


    public static OrderType of(int code) {

        switch (code) {
            case 1:
                return DOWN;
            case 2:
                return UP;
            case 3:
                return REBOUND;
            case 4:
                return RETRACEMENT;
            default:
                throw new IllegalArgumentException("unknown OrderType:" + code);
        }
    }
}
