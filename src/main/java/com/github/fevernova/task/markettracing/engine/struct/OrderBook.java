package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;


public abstract class OrderBook<T extends ConditionOrder> {


    protected final Map<Long, T> ordersMap = Maps.newHashMap();

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

        orderBook.ordersMap.values().forEach(t -> addOrder(t));
    }


    public abstract boolean addOrder(T order);


    public abstract boolean cancelOrder(long orderId);


    public abstract List<T> newPrice(double newPrice);


    protected void clear() {

        this.ordersMap.clear();
        this.downTree.clear();
        this.upTree.clear();
    }

}
