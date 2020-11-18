package com.github.fevernova.task.markettracing.data.order;


import lombok.Getter;


@Getter
public class SLOrder extends ConditionOrder {


    private Double triggerPrice;


    public SLOrder(Long orderId, OrderType orderType, Long userId, Long timestamp, Double triggerPrice) {

        super(orderId, orderType, userId, timestamp);
        this.triggerPrice = triggerPrice;
    }
}
