package com.github.fevernova.task.markettracing.engine;


import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.github.fevernova.task.markettracing.data.CandleMessage;
import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.SQTimeUtil;
import com.github.fevernova.task.markettracing.data.order.ConditionOrder;
import com.github.fevernova.task.markettracing.engine.struct.Factory;
import com.github.fevernova.task.markettracing.engine.struct.OrderBook;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

import java.util.*;


@Slf4j
public class TracingEngine<T extends OrderBook<E>, E extends ConditionOrder> implements WriteBytesMarshallable, ReadBytesMarshallable {


    public static final String CONS_NAME = "TracingEngine";

    private Map<Integer, CandleMessage> candlesCache = Maps.newHashMap();

    private Map<Integer, List<Market>> marketsCache = Maps.newHashMap();

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
            } else if (newCandle.getTimeSequence() < oldCandle.getTimeSequence()) {
                this.candlesCache.put(oldCandle.getPairCodeId(), oldCandle);
                return;
            }
        }
    }


    private void onFirst(CandleMessage candle) {

        final Long lastTickerTime = SQTimeUtil.toSequenceTime(candle.getTimeSequence(), candle.getTimestamp());
        this.lastTickerTimeMap.put(candle.getPairCodeId(), lastTickerTime);
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
        this.lastTickerTimeMap.put(newCandle.getPairCodeId(), lastTickerTime);
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

        List<Market> markets = getOrCreateMarkets(pairCodeId);
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
        //TODO 处理结果发送到下游
    }


    public void handleOrder(Integer pairCodeId, E order) {
        //条件单时间是历史的，先拿历史行情数据计算
        final Long lastTickerTime = this.lastTickerTimeMap.get(pairCodeId);
        if (Objects.nonNull(lastTickerTime) && order.getTimestamp() < lastTickerTime) {
            final List<Market> markets = getOrCreateMarkets(pairCodeId);
            final T t = this.factory.create();
            t.addOrder(order);
            markets.forEach(market -> {
                if (market.getTimestamp() > order.getTimestamp()) {
                    final List<E> result = t.process(market);
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


    private List<Market> getOrCreateMarkets(Integer pairCodeId) {

        List<Market> markets = this.marketsCache.get(pairCodeId);
        if (Objects.isNull(markets)) {
            markets = Lists.newLinkedList();
            this.marketsCache.put(pairCodeId, markets);
        }
        return markets;
    }


    private NavigableMap<Long, List<E>> getOrCreateOrderMap(Integer pairCodeId) {

        NavigableMap<Long, List<E>> orderMap = this.ordersCache.get(pairCodeId);
        if (Objects.isNull(orderMap)) {
            orderMap = Maps.newTreeMap();
            this.ordersCache.put(pairCodeId, orderMap);
        }
        return orderMap;
    }


    private T getOrCreateOrderBook(Integer pairCodeId) {

        T orderBook = this.orderBookMap.get(pairCodeId);
        if (Objects.isNull(orderBook)) {
            orderBook = this.factory.create();
            this.orderBookMap.put(pairCodeId, orderBook);
        }
        return orderBook;
    }


    public void heartbeat(Integer pairCodeId, Long timestamp) {

        final List<Market> markets = getOrCreateMarkets(pairCodeId);
        final Iterator<Market> each = markets.iterator();
        while (each.hasNext()) {
            if (timestamp - each.next().getTimestamp() > 3 * DateUtils.MILLIS_PER_MINUTE) {
                each.remove();
            } else {
                break;
            }
        }
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        Validate.isTrue(bytes.readInt() == 0);
        SerializationUtils.readIntMap(bytes, this.candlesCache, bytesIn -> new CandleMessage(bytesIn));
        SerializationUtils.readIntMap(bytes, this.marketsCache, bytesIn -> {

            List<Market> result = Lists.newLinkedList();
            SerializationUtils.readCollections(bytesIn, result, bytesIn1 -> new Market(bytesIn1));
            return result;
        });
        SerializationUtils.readIntMap(bytes, this.lastTickerTimeMap, bytesIn -> bytesIn.readLong());
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(0);
        SerializationUtils.writeIntMap(this.candlesCache, bytes);
        SerializationUtils.writeIntMap(bytes, this.marketsCache,
                                       (bytesOut, markets) -> SerializationUtils.writeCollections(bytesOut, markets, (bytesOut1, market) -> market.writeMarshallable(bytesOut1)));
        SerializationUtils.writeIntMap(bytes, this.lastTickerTimeMap, (bytesOut, v) -> bytesOut.writeLong(v));
    }
}
