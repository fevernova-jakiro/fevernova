package com.github.fevernova.task.exchange.engine;


import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;

import java.util.Collection;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;


@Slf4j
public class SerializationUtils {


    public static <T> void readIntMap(final BytesIn bytes, final Map<Integer, T> map, final Function<BytesIn, T> creator) {

        int length = bytes.readInt();
        for (int i = 0; i < length; i++) {
            int k = bytes.readInt();
            map.put(k, creator.apply(bytes));
        }
    }


    public static <T> void writeIntMap(final BytesOut bytes, final Map<Integer, T> map, final BiConsumer<BytesOut, T> creator) {

        bytes.writeInt(map.size());
        map.forEach((k, v) -> {
            bytes.writeInt(k);
            creator.accept(bytes, v);
        });
    }


    public static <T extends WriteBytesMarshallable> void writeIntMap(final BytesOut bytes, final Map<Integer, T> map) {

        writeIntMap(bytes, map, (bytesOut, t) -> t.writeMarshallable(bytesOut));
    }


    public static <T> void readCollections(final BytesIn bytes, final Collection<T> collection, final Function<BytesIn, T> creator) {

        int length = bytes.readInt();
        for (int i = 0; i < length; i++) {
            collection.add(creator.apply(bytes));
        }
    }


    public static <T> void writeCollections(final BytesOut bytes, final Collection<T> collection, final BiConsumer<BytesOut, T> creator) {

        bytes.writeInt(collection.size());
        collection.forEach(t -> creator.accept(bytes, t));
    }


    public static <T extends WriteBytesMarshallable> void writeCollections(final BytesOut bytes, final Collection<T> collection) {

        bytes.writeInt(collection.size());
        collection.forEach(t -> t.writeMarshallable(bytes));
    }
}
