package com.github.fevernova.task.markettracing.data.order;


import lombok.Getter;
import lombok.Setter;


@Getter
public class DTOrder extends ConditionOrder {


    @Setter
    private double polarPrice;

    private double deltaPrice;


    public DTOrder(Long orderId, OrderType orderType, Long userId, Long timestamp, Double polarPrice, Double deltaPrice) {

        super(orderId, orderType, userId, timestamp);
        this.polarPrice = polarPrice;
        this.deltaPrice = deltaPrice;
    }


    public double getTriggerPrice() {

        if (OrderType.RETRACEMENT == this.orderType) {
            return this.polarPrice - this.deltaPrice;
        } else {
            return this.polarPrice + this.deltaPrice;
        }
    }
}
