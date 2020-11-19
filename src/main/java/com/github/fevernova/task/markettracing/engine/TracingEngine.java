package com.github.fevernova.task.markettracing.engine;


import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.github.fevernova.task.markettracing.data.CandleMessage;
import com.github.fevernova.task.markettracing.data.Market;
import com.github.fevernova.task.markettracing.data.SQTimeUtil;
import com.github.fevernova.task.markettracing.data.TriggerResult;
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

    private Map<Integer, Long> lastTickerTimeMap = Maps.newHashMap();

    private Map<Integer, List<Market>> marketsCache = Maps.newHashMap();

    private Map<Integer, T> orderBookMap = Maps.newHashMap();

    private final Factory<T> factory;

    private final DataProvider<Integer, TriggerResult> provider;


    public TracingEngine(Factory<T> factory, DataProvider<Integer, TriggerResult> provider) {

        this.factory = factory;
        this.provider = provider;
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
        orderBook.loadPreOrders(market.getTimestamp());
        final List<E> result = orderBook.process(market);
        sendResult(pairCodeId, result);
    }


    public void handleOrder(Integer pairCodeId, E order) {

        OrderBook<E> orderBook = getOrCreateOrderBook(pairCodeId);
        final Long lastTickerTime = this.lastTickerTimeMap.get(pairCodeId);
        if (Objects.nonNull(lastTickerTime) && order.getTimestamp() < lastTickerTime) {
            final List<Market> markets = getOrCreateMarkets(pairCodeId);
            final T tmp = this.factory.create();
            tmp.addOrder(order);
            final List<E> result = Lists.newLinkedList();
            markets.forEach(market -> {
                if (market.getTimestamp() > order.getTimestamp()) {
                    result.addAll(tmp.process(market));
                }
            });
            sendResult(pairCodeId, result);
            orderBook.merge(tmp);
        } else {
            orderBook.addPreOrder(order);
        }
    }


    private void sendResult(int pairCodeId, List<E> result) {

        for (E e : result) {
            TriggerResult tr = this.provider.feedOne(pairCodeId);
            tr.from(pairCodeId, e);
            this.provider.push();
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
        SerializationUtils.readIntMap(bytes, this.lastTickerTimeMap, bytesIn -> bytesIn.readLong());
        SerializationUtils.readIntMap(bytes, this.marketsCache, bytesIn -> {

            List<Market> result = Lists.newLinkedList();
            SerializationUtils.readCollections(bytesIn, result, bytesIn1 -> new Market(bytesIn1));
            return result;
        });
        SerializationUtils.readIntMap(bytes, this.orderBookMap, bytesIn -> {

            T t = factory.create();
            t.readMarshallable(bytesIn);
            return t;
        });
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(0);
        SerializationUtils.writeIntMap(bytes, this.candlesCache);
        SerializationUtils.writeIntMap(bytes, this.lastTickerTimeMap, (bytesOut, v) -> bytesOut.writeLong(v));
        SerializationUtils.writeIntMap(bytes, this.marketsCache, (bytesOut, markets) -> SerializationUtils.writeCollections(bytesOut, markets));
        SerializationUtils.writeIntMap(bytes, this.orderBookMap);
    }
}
