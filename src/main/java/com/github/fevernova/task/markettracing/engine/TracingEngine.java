package com.github.fevernova.task.markettracing.engine;


import com.github.fevernova.task.markettracing.data.CandleMessage;
import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.SQTimeUtil;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import com.github.fevernova.task.markettracing.engine.struct.Factory;
import com.github.fevernova.task.markettracing.engine.struct.OrderBook;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static org.apache.commons.lang3.time.DateUtils.MILLIS_PER_MINUTE;


@Slf4j
public class TracingEngine<T extends OrderBook, E extends ConditionOrder> {


    private Map<Integer, CandleMessage> candlesCache = Maps.newHashMap();

    private Map<Integer, LinkedList<Market>> marketsCache = Maps.newHashMap();

    private Map<Integer, NavigableMap<Long, List<E>>> ordersCache = Maps.newHashMap();

    private Map<Integer, T> orderBookMap = Maps.newHashMap();

    private Map<Integer, Long> lastTickerTimeMap = Maps.newHashMap();

    private Factory<T> factory;


    public TracingEngine(Factory<T> factory) {

        this.factory = factory;
    }


    public void handleCandle(CandleMessage newCandle) {

        CandleMessage oldCandle = this.candlesCache.put(newCandle.getPairCodeId(), newCandle);
        if (oldCandle == null) {
            onFirst(newCandle);
        } else {
            if (newCandle.getTimeSequence().equals(oldCandle.getTimeSequence())) {
                onChange(oldCandle, newCandle);
            } else if (newCandle.getTimeSequence() > oldCandle.getTimeSequence()) {
                onFirst(newCandle);
            } else if (newCandle.getTimeSequence() < oldCandle.getTimeSequence()) {//忽略回退数据
                this.candlesCache.put(oldCandle.getPairCodeId(), oldCandle);
                return;
            }
        }
    }


    private void onFirst(CandleMessage candle) {

        final Long lastTickerTime = SQTimeUtil.toSequenceTime(candle.getTimeSequence(), candle.getTimestamp());
        lastTickerTimeMap.put(candle.getPairCodeId(), lastTickerTime);
        if (candle.getCount() == 0) {
            return;
        }
        List<Double> tickers;
        if (candle.getOpen() < candle.getClose()) {
            tickers = tracker(candle.getOpen(), candle.getLow(), candle.getHigh(), candle.getClose());
        } else {
            tickers = tracker(candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose());
        }
        processMarket(candle.getPairCodeId(), Market.builder().tickers(tickers).timestamp(lastTickerTime).build());
    }


    private void onChange(CandleMessage oldCandle, CandleMessage newCandle) {

        final Long lastTickerTime = SQTimeUtil.toSequenceTime(newCandle.getTimeSequence(), newCandle.getTimestamp());
        lastTickerTimeMap.put(newCandle.getPairCodeId(), lastTickerTime);
        if (oldCandle.getCount() >= newCandle.getCount()) {
            return;
        }
        Double nLow = oldCandle.getLow() > newCandle.getLow() ? newCandle.getLow() : null;
        Double nHigh = oldCandle.getHigh() < newCandle.getHigh() ? newCandle.getHigh() : null;
        List<Double> tickers;
        if (oldCandle.getClose() < newCandle.getClose()) {
            tickers = tracker(nLow, nHigh, newCandle.getClose());
        } else {
            tickers = tracker(nHigh, nLow, newCandle.getClose());
        }
        processMarket(newCandle.getPairCodeId(), Market.builder().tickers(tickers).timestamp(lastTickerTime).build());
    }


    private List<Double> tracker(Double... ps) {

        LinkedList<Double> result = Lists.newLinkedList();
        for (Double p : ps) {
            if (p != null) {
                Double last = result.peekLast();
                if (last == null || !p.equals(last)) {
                    result.add(p);
                }
            }
        }
        return result;
    }


    private void processMarket(Integer pairCodeId, Market market) {

        LinkedList<Market> markets = getOrCreateMarkets(pairCodeId);
        markets.add(market);

        final T orderBook = getOrCreateOrderBook(pairCodeId);

        final NavigableMap<Long, List<E>> orderMap = getOrCreateOrderMap(pairCodeId);
        final NavigableMap<Long, List<E>> moves = orderMap.headMap(market.getTimestamp(), false);
        final Iterator<Map.Entry<Long, List<E>>> iterator = moves.entrySet().iterator();
        while (iterator.hasNext()) {
            iterator.next().getValue().forEach(e -> orderBook.addOrder(e));
            iterator.remove();
        }
        final List<E> result = orderBook.process(market);
        result.forEach(e -> {
            System.out.println(e.getOrderId());
        });
        //TODO 处理结果发送到下游
    }


    public void handleOrder(Integer pairCodeId, E order) {
        //条件单时间是历史的，先拿历史行情数据计算
        final Long lastTickerTime = lastTickerTimeMap.get(pairCodeId);
        if (Objects.nonNull(lastTickerTime) && order.getTimestamp() < lastTickerTime) {
            final LinkedList<Market> markets = getOrCreateMarkets(pairCodeId);
            final T t = factory.create();
            t.addOrder(order);
            markets.forEach(market -> {
                if (market.getTimestamp() > order.getTimestamp()) {
                    final List<E> result = t.process(market);
                    result.forEach(e -> {
                        System.out.println(e.getOrderId());
                    });
                    //TODO 处理结果发送到下游
                }
            });
            getOrCreateOrderBook(pairCodeId).merge(t);
        } else {
            final NavigableMap<Long, List<E>> orderMap = getOrCreateOrderMap(pairCodeId);
            List<E> orderList = orderMap.get(order.getTimestamp());
            if (Objects.isNull(orderList)) {
                orderList = Lists.newLinkedList();
                orderMap.put(order.getTimestamp(), orderList);
            }
            orderList.add(order);
        }
    }


    private LinkedList<Market> getOrCreateMarkets(Integer pairCodeId) {

        LinkedList<Market> markets = marketsCache.get(pairCodeId);
        if (Objects.isNull(markets)) {
            markets = Lists.newLinkedList();
            marketsCache.put(pairCodeId, markets);
        }
        return markets;
    }


    private NavigableMap<Long, List<E>> getOrCreateOrderMap(Integer pairCodeId) {

        NavigableMap<Long, List<E>> orderMap = ordersCache.get(pairCodeId);
        if (Objects.isNull(orderMap)) {
            orderMap = Maps.newTreeMap();
            ordersCache.put(pairCodeId, orderMap);
        }
        return orderMap;
    }


    private T getOrCreateOrderBook(Integer pairCodeId) {

        T orderBook = orderBookMap.get(pairCodeId);
        if (Objects.isNull(orderBook)) {
            orderBook = factory.create();
            orderBookMap.put(pairCodeId, orderBook);
        }
        return orderBook;
    }


    public void heartbeat(Integer pairCodeId, Long timestamp) {

        final LinkedList<Market> markets = getOrCreateMarkets(pairCodeId);
        final Iterator<Market> each = markets.iterator();
        while (each.hasNext()) {
            if (timestamp - each.next().getTimestamp() > 3 * MILLIS_PER_MINUTE) {
                each.remove();
            } else {
                break;
            }
        }
    }
}
