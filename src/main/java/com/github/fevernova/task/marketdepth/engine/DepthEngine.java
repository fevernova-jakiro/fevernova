package com.github.fevernova.task.marketdepth.engine;


import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.github.fevernova.task.marketdepth.data.DepthResult;
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

    private int maxDepthSize;

    private long interval;

    private long lastScanTime = Util.nowMS();


    public DepthEngine(int maxDepthSize, long interval, DataProvider<Integer, DepthResult> provider) {

        this.maxDepthSize = maxDepthSize;
        this.interval = interval;
        this.provider = provider;
    }


    public void handle(OrderMatch match, long now) {

        SymbolDepths symbolDepths = this.data.get(match.getSymbolId());
        if (symbolDepths == null) {
            symbolDepths = new SymbolDepths(match.getSymbolId(), this.maxDepthSize);
            this.data.put(match.getSymbolId(), symbolDepths);
        }
        symbolDepths.handle(match, this.provider, now);
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
