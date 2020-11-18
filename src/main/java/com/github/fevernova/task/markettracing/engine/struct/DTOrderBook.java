package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.order.DTOrder;
import com.github.fevernova.task.markettracing.data.order.OrderType;
import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.*;


@Getter
public class DTOrderBook extends OrderBook<DTOrder> {


    private final NavigableMap<Double, Map<Long, DTOrder>> highPriceTree = Maps.newTreeMap();

    private final NavigableMap<Double, Map<Long, DTOrder>> lowPriceTree = Maps.newTreeMap((l1, l2) -> l2.compareTo(l1));


    @Override
    public boolean addOrder(DTOrder order) {

        order.setPolarPrice(lastPrice);
        DTOrder old = this.orders.putIfAbsent(order.getOrderId(), order);
        if (old != null) {
            return false;
        }
        if (OrderType.RETRACEMENT == order.getOrderType()) {
            add2TreeMap(order.getPolarPrice(), order, this.highPriceTree);
            add2TreeMap(order.getTriggerPrice(), order, this.downTree);
        } else {
            add2TreeMap(order.getPolarPrice(), order, this.lowPriceTree);
            add2TreeMap(order.getTriggerPrice(), order, this.upTree);
        }
        return true;
    }


    @Override
    public boolean cancelOrder(long orderId) {

        DTOrder order = this.orders.remove(orderId);
        if (order != null) {
            if (OrderType.RETRACEMENT == order.getOrderType()) {
                delFromTreeMap(order.getPolarPrice(), orderId, this.highPriceTree);
                delFromTreeMap(order.getTriggerPrice(), orderId, this.downTree);
            } else {
                delFromTreeMap(order.getPolarPrice(), orderId, this.lowPriceTree);
                delFromTreeMap(order.getTriggerPrice(), orderId, this.upTree);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<DTOrder> newPrice(double newPrice) {

        List<DTOrder> result = new LinkedList<>();
        if (this.lastPrice < newPrice) {
            adjust(newPrice, this.highPriceTree, this.downTree);
            match(result, newPrice, this.lowPriceTree, this.upTree);
        } else if (this.lastPrice > newPrice) {
            adjust(newPrice, this.lowPriceTree, this.upTree);
            match(result, newPrice, this.highPriceTree, this.downTree);
        }
        this.lastPrice = newPrice;
        return result;
    }


    private void adjust(double newPrice, NavigableMap<Double, Map<Long, DTOrder>> polarTree, NavigableMap<Double, Map<Long,
            DTOrder>> triggerTree) {

        SortedMap<Double, Map<Long, DTOrder>> moves = polarTree.headMap(newPrice, false);
        if (!moves.isEmpty()) {
            Map<Long, DTOrder> targetOrders = polarTree.get(newPrice);
            if (targetOrders == null) {
                targetOrders = Maps.newHashMap();
                polarTree.put(newPrice, targetOrders);
            }
            Iterator<Map.Entry<Double, Map<Long, DTOrder>>> iterator = moves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map<Long, DTOrder> tmpOrders = iterator.next().getValue();
                for (Map.Entry<Long, DTOrder> entry : tmpOrders.entrySet()) {
                    DTOrder order = entry.getValue();
                    targetOrders.put(order.getOrderId(), order);
                    delFromTreeMap(order.getTriggerPrice(), order.getOrderId(), triggerTree);
                    order.setPolarPrice(newPrice);
                    add2TreeMap(order.getTriggerPrice(), order, triggerTree);
                }
                iterator.remove();
            }
        }
    }


    private void match(List<DTOrder> result, double newPrice, NavigableMap<Double, Map<Long, DTOrder>> polarTree,
                       NavigableMap<Double, Map<Long, DTOrder>> triggerTree) {

        SortedMap<Double, Map<Long, DTOrder>> moves = triggerTree.tailMap(newPrice, true);
        if (!moves.isEmpty()) {
            Iterator<Map.Entry<Double, Map<Long, DTOrder>>> iterator = moves.entrySet().iterator();
            while (iterator.hasNext()) {
                Map<Long, DTOrder> tmpOrders = iterator.next().getValue();
                for (Map.Entry<Long, DTOrder> entry : tmpOrders.entrySet()) {
                    DTOrder order = entry.getValue();
                    result.add(order);
                    delFromTreeMap(order.getPolarPrice(), order.getOrderId(), polarTree);
                    this.orders.remove(order.getOrderId());
                }
                iterator.remove();
            }
        }
    }


    @Override
    public void clear() {

        super.clear();
        this.highPriceTree.clear();
        this.lowPriceTree.clear();
    }
}
