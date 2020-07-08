package com.github.fevernova.task.exchange.data.order;


public enum OrderType {
    GTC(0),//限价
    IOC(1),//限价市价
    FOK(2),//全部成交或者取消
    POSTONLY(3);//只做Maker

    public byte code;


    OrderType(int code) {

        this.code = (byte) code;
    }


    public static OrderType of(int code) {

        switch (code) {
            case 0:
                return GTC;
            case 1:
                return IOC;
            case 2:
                return FOK;
            case 3:
                return POSTONLY;
            default:
                throw new IllegalArgumentException("Unknown OrderType:" + code);
        }
    }

}
