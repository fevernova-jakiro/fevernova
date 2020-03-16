package com.github.fevernova.task.exchange.data.cmd;


public enum OrderCommandType {

    PLACE_ORDER(0),
    CANCEL_ORDER(1),
    HEARTBEAT(2);

    public byte code;


    OrderCommandType(int code) {

        this.code = (byte) code;
    }


    public static OrderCommandType of(int code) {

        switch (code) {
            case 0:
                return PLACE_ORDER;
            case 1:
                return CANCEL_ORDER;
            case 2:
                return HEARTBEAT;
            default:
                throw new IllegalArgumentException("unknown OrderCommandType:" + code);
        }
    }

}
