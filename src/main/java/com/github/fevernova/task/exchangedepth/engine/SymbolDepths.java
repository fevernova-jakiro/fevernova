package com.github.fevernova.task.exchangedepth.engine;


import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.order.OrderAction;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.data.result.OrderPart;
import com.github.fevernova.task.exchange.data.result.ResultCode;
import com.github.fevernova.task.exchangedepth.books.AskDepthBooks;
import com.github.fevernova.task.exchangedepth.books.BidDepthBooks;
import com.github.fevernova.task.exchangedepth.books.DepthBooks;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;


@NoArgsConstructor
@Getter
public class SymbolDepths implements WriteBytesMarshallable, ReadBytesMarshallable {


    private int symbolId;

    private int maxDepthSize;

    private DepthBooks bids = new BidDepthBooks();

    private DepthBooks asks = new AskDepthBooks();

    private long lastSequence;

    private boolean update = false;

    private long lastDumpTime = Util.nowMS();


    public SymbolDepths(int symbolId, int maxDepthSize) {

        this.symbolId = symbolId;
        this.maxDepthSize = maxDepthSize;
    }


    public void handle(OrderMatch match, DataProvider<Integer, DepthResult> provider, long now) {

        if (this.lastSequence >= match.maxSeq()) {
            return;
        }
        this.lastSequence = match.maxSeq();

        OrderPart part1 = match.getOrderPart1();
        DepthBooks depthBooks0 = OrderAction.BID == part1.getOrderAction() ? this.bids : this.asks;
        depthBooks0.handle(part1.getOrderPrice(), part1.getOrderPriceDepthSize(), part1.getOrderPriceOrderCount());

        if (ResultCode.MATCH == match.getResultCode()) {
            OrderPart part2 = match.getOrderPart2();
            DepthBooks depthBooks1 = OrderAction.BID == part2.getOrderAction() ? this.bids : this.asks;
            depthBooks1.handle(part2.getOrderPrice(), part2.getOrderPriceDepthSize(), part2.getOrderPriceOrderCount());
        }

        this.update = true;
        scan(provider, now);
    }


    public void scan(DataProvider<Integer, DepthResult> provider, long now) {

        if (this.bids.newEdgePrice(this.asks.getCachePrice()) && needDump(now)) {
            DepthResult depthResult = provider.feedOne(this.symbolId);
            depthResult.setSymbolId(this.symbolId);
            depthResult.setTimestamp(now);
            depthResult.dump(this, this.maxDepthSize);
            provider.push();
            this.update = false;
            this.lastDumpTime = now;
        }
    }


    private boolean needDump(long now) {

        if (now - this.lastDumpTime >= 60 * 1000L) {
            return true;
        } else if (this.update && now - this.lastDumpTime >= 1000L) {
            return true;
        }
        return false;
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        this.symbolId = bytes.readInt();
        this.maxDepthSize = bytes.readInt();
        this.bids.readMarshallable(bytes);
        this.asks.readMarshallable(bytes);
        this.lastSequence = bytes.readLong();
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(this.symbolId);
        bytes.writeInt(this.maxDepthSize);
        this.bids.writeMarshallable(bytes);
        this.asks.writeMarshallable(bytes);
        bytes.writeLong(this.lastSequence);
    }
}
