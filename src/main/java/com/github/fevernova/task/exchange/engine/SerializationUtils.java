package com.github.fevernova.task.exchange.engine;


import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.util.Map;
import java.util.function.Function;


@Slf4j
public class SerializationUtils {


    public static <T extends WriteBytesMarshallable> void writeIntMap(final Map<Integer, T> map, final BytesOut bytes) {

        bytes.writeInt(map.size());
        map.forEach((k, v) -> {
            bytes.writeInt(k);
            v.writeMarshallable(bytes);
        });
    }


    public static <T> void readIntMap(final BytesIn bytes, final Map<Integer, T> map, final Function<BytesIn, T> creator) {

        int length = bytes.readInt();
        for (int i = 0; i < length; i++) {
            int k = bytes.readInt();
            map.put(k, creator.apply(bytes));
        }
    }
}
