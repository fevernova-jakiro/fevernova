package com.github.fevernova.task.track;


import com.google.common.collect.Maps;
import lombok.Getter;

import java.util.*;


public class TrackTree {


    @Getter
    private final NavigableMap<Long, NavigableMap<Long, List<Long>>> tree = Maps.newTreeMap();

    @Getter
    private long lastPrice;


    public List<Long> newPrice(long newPrice) {

        List<Long> result = new LinkedList<>();
        if (this.lastPrice > newPrice) {
            Iterator<Map.Entry<Long, NavigableMap<Long, List<Long>>>> iterator = this.tree.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, NavigableMap<Long, List<Long>>> entry = iterator.next();
                long delta = entry.getKey() - newPrice;
                NavigableMap<Long, List<Long>> subTree = entry.getValue();
                SortedMap<Long, List<Long>> moves = subTree.headMap(delta, true);
                if (!moves.isEmpty()) {
                    Iterator<Map.Entry<Long, List<Long>>> entryIterator = moves.entrySet().iterator();
                    while (entryIterator.hasNext()) {
                        Map.Entry<Long, List<Long>> item = entryIterator.next();
                        result.addAll(item.getValue());
                        entryIterator.remove();
                    }
                    if (subTree.isEmpty()) {
                        iterator.remove();
                    }
                }
            }
        } else {
            SortedMap<Long, NavigableMap<Long, List<Long>>> moves = this.tree.headMap(newPrice);
            if (!moves.isEmpty()) {
                NavigableMap<Long, List<Long>> targetSubTree = this.tree.get(newPrice);
                if (targetSubTree == null) {
                    targetSubTree = Maps.newTreeMap();
                    this.tree.put(newPrice, targetSubTree);
                }
                Iterator<Map.Entry<Long, NavigableMap<Long, List<Long>>>> iterator = moves.entrySet().iterator();
                while (iterator.hasNext()) {
                    NavigableMap<Long, List<Long>> sourceSubTree = iterator.next().getValue();
                    for (Map.Entry<Long, List<Long>> entry : sourceSubTree.entrySet()) {
                        List<Long> orderIds = targetSubTree.get(entry.getKey());
                        if (orderIds == null) {
                            orderIds = new LinkedList<>();
                            targetSubTree.put(entry.getKey(), orderIds);
                        }
                        orderIds.addAll(entry.getValue());
                    }
                    iterator.remove();
                }
            }
        }
        this.lastPrice = newPrice;
        return result;
    }


    public void addOrder(long orderId, long deltaPrice) {

        NavigableMap<Long, List<Long>> subTree = this.tree.get(this.lastPrice);
        List<Long> orderIds = null;
        if (subTree != null) {
            orderIds = subTree.get(deltaPrice);
        } else {
            subTree = Maps.newTreeMap();
            this.tree.put(this.lastPrice, subTree);
        }
        if (orderIds == null) {
            orderIds = new LinkedList<>();
            subTree.put(deltaPrice, orderIds);
        }
        orderIds.add(orderId);
    }


    public void cancelOrder(long orderId, long deltaPrice) {

        Iterator<NavigableMap<Long, List<Long>>> iterator = this.tree.values().iterator();
        while (iterator.hasNext()) {
            NavigableMap<Long, List<Long>> subTree = iterator.next();
            List<Long> orderIds = subTree.get(deltaPrice);
            if (orderIds != null && orderIds.size() > 0) {
                if (orderIds.remove(orderId)) {
                    if (orderIds.isEmpty()) {
                        subTree.remove(deltaPrice);
                        if (subTree.isEmpty()) {
                            iterator.remove();
                        }
                    }
                    break;
                }
            }
        }
    }


    public void clear() {

        this.tree.clear();
    }

}
