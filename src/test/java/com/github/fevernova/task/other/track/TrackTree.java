package com.github.fevernova.task.other.track;


import com.google.common.collect.Maps;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.*;


public class TrackTree {


    @Getter
    private final Map<Long, Order> ordersMap = Maps.newHashMap();

    private final NavigableMap<Long, Map<Long, Order>> highPriceTree = Maps.newTreeMap();

    private final NavigableMap<Long, Map<Long, Order>> lowPriceTree = Maps.newTreeMap();

    @Getter
    private long lastPrice;


    public List<Long> newPrice(long newPrice) {

        List<Long> result = new LinkedList<>();
        if (this.lastPrice < newPrice) {
            SortedMap<Long, Map<Long, Order>> moves = this.highPriceTree.headMap(newPrice);
            if (!moves.isEmpty()) {
                Map<Long, Order> targetOrders = this.highPriceTree.get(newPrice);
                if (targetOrders == null) {
                    targetOrders = Maps.newHashMap();
                    this.highPriceTree.put(newPrice, targetOrders);
                }
                Iterator<Map.Entry<Long, Map<Long, Order>>> iterator = moves.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map<Long, Order> tmpOrders = iterator.next().getValue();
                    for (Map.Entry<Long, Order> entry : tmpOrders.entrySet()) {
                        Order order = entry.getValue();
                        targetOrders.put(order.getOrderId(), order);
                        deleteFromTreeMap(order.getLowPrice(), order.getOrderId(), this.lowPriceTree);
                        order.setHighPrice(newPrice);
                        add2TreeMap(order.getLowPrice(), order, this.lowPriceTree);
                    }
                    iterator.remove();
                }
            }
        } else if (this.lastPrice > newPrice) {
            SortedMap<Long, Map<Long, Order>> moves = this.lowPriceTree.tailMap(newPrice, true);
            if (!moves.isEmpty()) {
                Iterator<Map.Entry<Long, Map<Long, Order>>> iterator = moves.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map<Long, Order> tmpOrders = iterator.next().getValue();
                    for (Map.Entry<Long, Order> entry : tmpOrders.entrySet()) {
                        Order order = entry.getValue();
                        result.add(order.getOrderId());
                        deleteFromTreeMap(order.getHighPrice(), order.getOrderId(), this.highPriceTree);
                        this.ordersMap.remove(order.getOrderId());
                    }
                    iterator.remove();
                }
            }
        }
        this.lastPrice = newPrice;
        return result;
    }


    public boolean addOrder(long orderId, long deltaPrice) {

        Order order = Order.builder().highPrice(this.lastPrice).deltaPrice(deltaPrice).orderId(orderId).build();
        Order old = this.ordersMap.putIfAbsent(order.getOrderId(), order);
        if (old != null) {
            return false;
        }

        add2TreeMap(order.getHighPrice(), order, this.highPriceTree);
        add2TreeMap(order.getLowPrice(), order, this.lowPriceTree);
        return true;
    }


    public boolean cancelOrder(long orderId) {

        Order order = this.ordersMap.remove(orderId);
        if (order != null) {
            deleteFromTreeMap(order.getHighPrice(), orderId, this.highPriceTree);
            deleteFromTreeMap(order.getLowPrice(), orderId, this.lowPriceTree);
            return true;
        } else {
            return false;
        }
    }


    private void add2TreeMap(long price, Order order, NavigableMap<Long, Map<Long, Order>> tree) {

        Map<Long, Order> map = tree.get(price);
        if (map == null) {
            map = Maps.newHashMap();
            tree.put(price, map);
        }
        map.put(order.getOrderId(), order);
    }


    private void deleteFromTreeMap(long price, long orderId, NavigableMap<Long, Map<Long, Order>> tree) {

        Map<Long, Order> map = tree.get(price);
        if (map != null) {
            map.remove(orderId);
            if (map.isEmpty()) {
                tree.remove(price);
            }
        }
    }


    public void clear() {

        this.ordersMap.clear();
        this.highPriceTree.clear();
        this.lowPriceTree.clear();
    }


    @Getter
    @Builder
    @ToString
    static class Order {


        @Setter
        private long highPrice;

        private long deltaPrice;

        private long orderId;


        public long getLowPrice() {

            return this.highPrice - this.deltaPrice;
        }
    }

}
