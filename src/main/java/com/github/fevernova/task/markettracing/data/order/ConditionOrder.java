package com.github.fevernova.task.markettracing.data.order;


import lombok.Getter;


@Getter
public class ConditionOrder {


    protected Long orderId;

    protected OrderType orderType;

    protected Long userId;

    protected Long timestamp;


    public ConditionOrder(Long orderId, OrderType orderType, Long userId, Long timestamp) {

        this.orderId = orderId;
        this.orderType = orderType;
        this.userId = userId;
        this.timestamp = timestamp;
    }
}
