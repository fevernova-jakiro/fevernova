package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.*;


public abstract class OrderBook<T extends ConditionOrder> {


    protected final Map<Long, T> orders = Maps.newHashMap();

    protected final NavigableMap<Long, List<T>> preOrders = Maps.newTreeMap();

    protected final NavigableMap<Double, Map<Long, T>> downTree = Maps.newTreeMap();

    protected final NavigableMap<Double, Map<Long, T>> upTree = Maps.newTreeMap((l1, l2) -> l2.compareTo(l1));

    protected double lastPrice;


    protected void add2TreeMap(double price, T order, NavigableMap<Double, Map<Long, T>> tree) {

        Map<Long, T> map = tree.get(price);
        if (map == null) {
            map = Maps.newHashMap();
            tree.put(price, map);
        }
        map.put(order.getOrderId(), order);
    }


    protected void delFromTreeMap(double price, long orderId, NavigableMap<Double, Map<Long, T>> tree) {

        Map<Long, T> map = tree.get(price);
        if (map != null) {
            map.remove(orderId);
            if (map.isEmpty()) {
                tree.remove(price);
            }
        }
    }


    public List<T> process(Market market) {

        final List<T> result = Lists.newLinkedList();
        market.getTickers().forEach(d -> {
            result.addAll(newPrice(d));
        });
        return result;
    }


    public void merge(OrderBook<T> orderBook) {

        orderBook.orders.values().forEach(t -> addOrder(t));
    }


    public void addPreOrder(T order) {

        List<T> orderList = this.preOrders.get(order.getTimestamp());
        if (Objects.isNull(orderList)) {
            orderList = Lists.newLinkedList();
            this.preOrders.put(order.getTimestamp(), orderList);
        }
        orderList.add(order);
    }


    public void loadPreOrders(long timestamp) {

        final NavigableMap<Long, List<T>> moves = preOrders.headMap(timestamp, false);
        final Iterator<Map.Entry<Long, List<T>>> iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().forEach(e -> addOrder(e));
            iterator.remove();
        }
    }


    public abstract boolean addOrder(T order);

    public abstract boolean cancelOrder(long orderId);


    public abstract List<T> newPrice(double newPrice);


    protected void clear() {

        this.orders.clear();
        this.downTree.clear();
        this.upTree.clear();
    }

}
