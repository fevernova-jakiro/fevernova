package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.order.DTOrder;
import com.github.fevernova.task.markettracing.data.order.OrderType;
import com.google.common.collect.Maps;
import lombok.Getter;
import net.openhft.chronicle.bytes.BytesIn;

import java.util.*;


@Getter
public class DTOrderBook extends OrderBook<DTOrder> {


    private final NavigableMap<Double, Map<Long, DTOrder>> highPriceTree = Maps.newTreeMap();

    private final NavigableMap<Double, Map<Long, DTOrder>> lowPriceTree = Maps.newTreeMap((l1, l2) -> l2.compareTo(l1));


    @Override
    public boolean addOrder(DTOrder order) {

        order.setPolarPrice(super.lastPrice);
        DTOrder old = super.orders.putIfAbsent(order.getOrderId(), order);
        if (old != null) {
            return false;
        }
        if (OrderType.RETRACEMENT == order.getOrderType()) {
            add2TreeMap(order.getPolarPrice(), order, this.highPriceTree);
            add2TreeMap(order.getTriggerPrice(), order, super.downTree);
        } else {
            add2TreeMap(order.getPolarPrice(), order, this.lowPriceTree);
            add2TreeMap(order.getTriggerPrice(), order, super.upTree);
        }
        return true;
    }


    @Override
    public boolean cancelOrder(long orderId) {

        if (cancelPreOrder(orderId)) {
            return true;
        }
        DTOrder order = super.orders.remove(orderId);
        if (order != null) {
            if (OrderType.RETRACEMENT == order.getOrderType()) {
                delFromTreeMap(order.getPolarPrice(), orderId, this.highPriceTree);
                delFromTreeMap(order.getTriggerPrice(), orderId, super.downTree);
            } else {
                delFromTreeMap(order.getPolarPrice(), orderId, this.lowPriceTree);
                delFromTreeMap(order.getTriggerPrice(), orderId, super.upTree);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<DTOrder> newPrice(double newPrice) {

        List<DTOrder> result = new LinkedList<>();
        if (super.lastPrice < newPrice) {
            adjust(newPrice, this.highPriceTree, super.downTree);
            match(result, newPrice, this.lowPriceTree, super.upTree);
        } else if (super.lastPrice > newPrice) {
            adjust(newPrice, this.lowPriceTree, super.upTree);
            match(result, newPrice, this.highPriceTree, super.downTree);
        }
        super.lastPrice = newPrice;
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
                    super.orders.remove(order.getOrderId());
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


    @Override protected DTOrder newOrder(BytesIn bytes) {

        return new DTOrder(bytes);
    }
}
