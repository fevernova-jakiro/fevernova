package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.order.OrderType;
import com.github.fevernova.task.markettracing.data.order.SLOrder;

import java.util.*;


public class SLOrderBook extends OrderBook<SLOrder> {


    @Override
    public boolean addOrder(SLOrder order) {

        final SLOrder old = this.orders.putIfAbsent(order.getOrderId(), order);
        if (old != null) {
            return false;
        }
        if (OrderType.DOWN == order.getOrderType()) {
            add2TreeMap(order.getTriggerPrice(), order, this.downTree);
        } else {
            add2TreeMap(order.getTriggerPrice(), order, this.upTree);
        }
        return true;
    }


    @Override
    public boolean cancelOrder(long orderId) {

        final SLOrder order = this.orders.remove(orderId);
        if (order != null) {
            if (OrderType.DOWN == order.getOrderType()) {
                delFromTreeMap(order.getTriggerPrice(), orderId, this.downTree);
            } else {
                delFromTreeMap(order.getTriggerPrice(), orderId, this.upTree);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<SLOrder> newPrice(double newPrice) {

        List<SLOrder> result = new LinkedList<>();
        if (this.lastPrice < newPrice) {
            match(result, newPrice, this.upTree);
        } else if (this.lastPrice > newPrice) {
            match(result, newPrice, this.downTree);
        }
        this.lastPrice = newPrice;
        return result;
    }


    private void match(List<SLOrder> result, double newPrice, NavigableMap<Double, Map<Long, SLOrder>> triggerTree) {

        NavigableMap<Double, Map<Long, SLOrder>> moves = triggerTree.tailMap(newPrice, true);
        if (!moves.isEmpty()) {
            Iterator<Map.Entry<Double, Map<Long, SLOrder>>> iterator = moves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map<Long, SLOrder> tmpOrders = iterator.next().getValue();
                for (Map.Entry<Long, SLOrder> entry : tmpOrders.entrySet()) {
                    final SLOrder order = entry.getValue();
                    result.add(order);
                    this.orders.remove(order.getOrderId());
                }
                iterator.remove();
            }
        }
    }
}
