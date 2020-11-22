package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.order.OrderType;
import com.github.fevernova.task.markettracing.data.order.SLOrder;
import net.openhft.chronicle.bytes.BytesIn;

import java.util.*;


public class SLOrderBook extends OrderBook<SLOrder> {


    @Override
    public boolean addOrder(SLOrder order) {

        final SLOrder old = super.orders.putIfAbsent(order.getOrderId(), order);
        if (old != null) {
            return false;
        }
        if (OrderType.DOWN == order.getOrderType()) {
            add2TreeMap(order.getTriggerPrice(), order, super.downTree);
        } else {
            add2TreeMap(order.getTriggerPrice(), order, super.upTree);
        }
        return true;
    }


    @Override
    public boolean cancelOrder(long orderId) {

        if (cancelPreOrder(orderId)) {
            return true;
        }
        final SLOrder order = super.orders.remove(orderId);
        if (order != null) {
            if (OrderType.DOWN == order.getOrderType()) {
                delFromTreeMap(order.getTriggerPrice(), orderId, super.downTree);
            } else {
                delFromTreeMap(order.getTriggerPrice(), orderId, super.upTree);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<SLOrder> newPrice(double newPrice) {

        super.lastPrice = newPrice;
        List<SLOrder> result = new LinkedList<>();
        match(result, newPrice, super.upTree);
        match(result, newPrice, super.downTree);
        return result;
    }


    @Override protected SLOrder newOrder(BytesIn bytes) {

        return new SLOrder(bytes);
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
                    super.orders.remove(order.getOrderId());
                }
                iterator.remove();
            }
        }
    }
}
