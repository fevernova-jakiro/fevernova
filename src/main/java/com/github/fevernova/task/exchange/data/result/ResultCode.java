package com.github.fevernova.task.exchange.data.result;


public enum ResultCode {

    PLACE(10),

    CANCEL(20),
    CANCEL_IOC(21),
    CANCEL_FOK(22),
    CANCEL_POSTONLY(23),
    CANCEL_NOTFOUND(24),

    MATCH(30),

    HEARTBEAT(40);

    public int code;


    ResultCode(int code) {

        this.code = code;
    }


    public static ResultCode of(int code) {

        switch (code) {
            case 10:
                return PLACE;
            case 20:
                return CANCEL;
            case 21:
                return CANCEL_IOC;
            case 22:
                return CANCEL_FOK;
            case 23:
                return CANCEL_POSTONLY;
            case 30:
                return MATCH;
            case 40:
                return HEARTBEAT;
            default:
                throw new IllegalArgumentException("unknown ResultCode:" + code);
        }
    }
}
