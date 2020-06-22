package com.github.fevernova.task.exchangedepth.engine;


import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import com.google.common.collect.Maps;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.apache.commons.lang3.Validate;

import java.util.Map;


public class DepthEngine implements WriteBytesMarshallable, ReadBytesMarshallable {


    public static final String CONS_NAME = "DepthData";

    private final DataProvider<Integer, DepthResult> provider;

    private Map<Integer, SymbolDepths> data = Maps.newHashMap();

    private SymbolDepths lastSymbolDepths;

    private int maxDepthSize;

    private long interval;

    private long lastScanTime = Util.nowMS();


    public DepthEngine(int maxDepthSize, long interval, DataProvider<Integer, DepthResult> provider) {

        this.maxDepthSize = maxDepthSize;
        this.interval = interval;
        this.provider = provider;
    }


    public void handle(OrderMatch match, long now) {

        int symbolId = match.getSymbolId();
        if (this.lastSymbolDepths == null || this.lastSymbolDepths.getSymbolId() != symbolId) {
            this.lastSymbolDepths = this.data.get(symbolId);
            if (this.lastSymbolDepths == null) {
                this.lastSymbolDepths = new SymbolDepths(symbolId, this.maxDepthSize);
                this.data.put(symbolId, this.lastSymbolDepths);
            }
        }
        this.lastSymbolDepths.handle(match, this.provider, now);
    }


    public void scan(long now) {

        if (now - this.lastScanTime >= this.interval) {
            this.lastScanTime = now;
        }
        this.data.forEach((id, symbolDepths) -> symbolDepths.scan(provider, now));
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        Validate.isTrue(bytes.readInt() == 0);
        SerializationUtils.readIntMap(bytes, this.data, bytesIn -> {

            SymbolDepths symbolDepths = new SymbolDepths();
            symbolDepths.readMarshallable(bytesIn);
            return symbolDepths;
        });
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(0);
        SerializationUtils.writeIntMap(this.data, bytes);
    }
}
