package com.github.fevernova.task.markettracing.engine.struct;


import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.util.*;


public abstract class OrderBook<T extends ConditionOrder> implements WriteBytesMarshallable, ReadBytesMarshallable {


    protected double lastPrice;

    protected final Map<Long, T> orders = Maps.newHashMap();

    protected final Map<Long, T> preOrders = Maps.newHashMap();

    protected final NavigableMap<Long, List<T>> preOrdersTree = Maps.newTreeMap();

    protected final NavigableMap<Double, Map<Long, T>> downTree = Maps.newTreeMap();

    protected final NavigableMap<Double, Map<Long, T>> upTree = Maps.newTreeMap((l1, l2) -> l2.compareTo(l1));


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
        market.getTickers().forEach(d -> result.addAll(newPrice(d)));
        return result;
    }


    public void merge(OrderBook<T> orderBook) {

        if (!orderBook.orders.isEmpty()) {
            orderBook.orders.values().forEach(t -> addOrder(t));
        }
    }


    public void addPreOrder(T order) {

        this.preOrders.put(order.getOrderId(), order);
        List<T> orderList = this.preOrdersTree.get(order.getTimestamp());
        if (Objects.isNull(orderList)) {
            orderList = Lists.newLinkedList();
            this.preOrdersTree.put(order.getTimestamp(), orderList);
        }
        orderList.add(order);
    }


    public boolean cancelPreOrder(long orderId) {

        ConditionOrder conditionOrder = this.preOrders.remove(orderId);
        if (conditionOrder == null) {
            return false;
        }
        List<T> some = this.preOrdersTree.get(conditionOrder.getTimestamp());
        for (Iterator<T> it = some.iterator(); it.hasNext(); ) {
            T order = it.next();
            if (orderId == order.getOrderId()) {
                it.remove();
                break;
            }
        }
        return true;
    }


    public void loadPreOrders(long timestamp) {

        final NavigableMap<Long, List<T>> moves = this.preOrdersTree.headMap(timestamp, false);
        final Iterator<Map.Entry<Long, List<T>>> iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().forEach(e -> {
                addOrder(e);
                preOrders.remove(e.getOrderId());
            });
            iterator.remove();
        }
    }


    public abstract boolean addOrder(T order);

    public abstract boolean cancelOrder(long orderId);

    public abstract List<T> newPrice(double newPrice);


    protected abstract T newOrder(BytesIn bytes);


    protected void clear() {

        this.orders.clear();
        this.downTree.clear();
        this.upTree.clear();
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        this.lastPrice = bytes.readDouble();
        List<T> tmpOrders = Lists.newLinkedList();
        SerializationUtils.readCollections(bytes, tmpOrders, bytesIn -> newOrder(bytesIn));
        tmpOrders.forEach(t -> addOrder(t));
        tmpOrders.clear();
        SerializationUtils.readCollections(bytes, tmpOrders, bytesIn -> newOrder(bytesIn));
        tmpOrders.forEach(t -> addPreOrder(t));
        tmpOrders.clear();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeDouble(this.lastPrice);
        SerializationUtils.writeCollections(bytes, this.orders.values());
        SerializationUtils.writeCollections(bytes, this.preOrders.values());
    }
}
