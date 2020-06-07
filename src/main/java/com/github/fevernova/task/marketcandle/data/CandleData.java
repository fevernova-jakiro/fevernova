package com.github.fevernova.task.marketcandle.data;


import com.github.fevernova.task.exchange.data.result.OrderMatch;
import com.github.fevernova.task.exchange.engine.SerializationUtils;
import com.google.common.collect.Maps;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.apache.commons.lang3.Validate;

import java.util.Map;


public class CandleData implements WriteBytesMarshallable, ReadBytesMarshallable {


    public static final String CONS_NAME = "CandleData";

    private Map<Integer, CandleLine> data = Maps.newHashMap();


    public void handle(OrderMatch orderMatch, INotify notify) {

        CandleLine line = getOrCreateLine(orderMatch.getSymbolId());
        line.acc(orderMatch.getTimestamp(), orderMatch.getMatchPart().getMatchPrice(), orderMatch.getMatchPart().getMatchSize(), orderMatch.maxSeq(),
                 notify);
    }


    private CandleLine getOrCreateLine(int symbolId) {

        CandleLine line = this.data.get(symbolId);
        if (line == null) {
            line = new CandleLine(symbolId);
            this.data.put(symbolId, line);
        }
        return line;
    }


    public void scan4Update(boolean repair, INotify notify, long now) {

        this.data.forEach((id, line) -> line.scan4Update(repair, notify, now));
    }


    @Override public void writeMarshallable(BytesOut bytes) {

        bytes.writeInt(0);
        SerializationUtils.writeIntMap(this.data, bytes);
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        Validate.isTrue(bytes.readInt() == 0);
        SerializationUtils.readIntMap(bytes, this.data, bytesIn -> {

            CandleLine line = new CandleLine(0);
            line.readMarshallable(bytesIn);
            return line;
        });
    }
}
