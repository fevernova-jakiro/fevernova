package com.github.fevernova.data.message;


import org.apache.avro.Schema;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;


public class SerializerHelper implements Deserializer<DataContainer>, Serializer<DataContainer> {


    private final static Schema SIMPLESCHEMA = new Schema.Parser()
            .parse("{\"namespace\":\"com.github.fevernova.data.message\",\"type\":\"record\",\"name\":\"Data\","
                   + "\"fields\":[{\"name\":\"nid\",\"type\":\"long\"},{\"name\":\"sid\",\"type\":[\"null\",\"string\"]},"
                   + "{\"name\":\"opt\",\"type\":{\"name\":\"Opt\",\"type\":\"enum\",\"symbols\":[\"INSERT\",\"UPDATE\","
                   + "\"DELETE\"]}},{\"name\":\"timestamp\",\"type\":\"long\"},{\"name\":\"tags\",\"type\":[{\"type\":\"map\","
                   + "\"values\":[\"null\",\"string\"]},\"null\"]},{\"name\":\"metaId\",\"type\":\"long\"},"
                   + "{\"name\":\"updatesLong\",\"type\":\"long\"}]}");

    private DatumWriter<Data> writer = new SpecificDatumWriter<>(Data.getClassSchema());

    private BinaryEncoder binaryEncoder;

    private int encoderBufferSize;

    private EncoderFactory encoderFactory = new EncoderFactory();

    private int byteArrayBaseSize;

    private DatumReader<Data> reader = new SpecificDatumReader<>(Data.getClassSchema());

    private BinaryDecoder binaryDecoder;

    private DatumReader<Data> reader4Headers = new SpecificDatumReader<>(SIMPLESCHEMA);

    private BinaryDecoder binaryDecoder4Headers;

    private int decoderBufferSize;

    private DecoderFactory decoderFactory = new DecoderFactory();


    public SerializerHelper() {

        this(2048, 1024, 4096);
    }


    public SerializerHelper(int encoderBufferSize, int byteArrayBaseSize, int decoderBufferSize) {

        this.encoderBufferSize = encoderBufferSize;
        this.byteArrayBaseSize = byteArrayBaseSize;
        this.encoderFactory.configureBufferSize(this.encoderBufferSize);
        this.decoderBufferSize = decoderBufferSize;
        this.decoderFactory.configureDecoderBufferSize(this.decoderBufferSize);
    }


    @Override
    public byte[] serialize(String s, DataContainer data) {

        try (ByteArrayOutputStream os = new ByteArrayOutputStream(this.byteArrayBaseSize)) {
            this.binaryEncoder = this.encoderFactory.binaryEncoder(os, this.binaryEncoder);
            this.writer.write(data.getData(), this.binaryEncoder);
            this.binaryEncoder.flush();
            return os.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public DataContainer deserialize(String s, byte[] bytes) {

        this.binaryDecoder = this.decoderFactory.binaryDecoder(bytes, this.binaryDecoder);
        try {
            Data r = this.reader.read(null, this.binaryDecoder);
            return DataContainer.createDataContainer4Read(null, r);//TODO get meta
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public DataContainer localDeserialize(String s, byte[] bytes) {

        this.binaryDecoder4Headers = this.decoderFactory.binaryDecoder(bytes, this.binaryDecoder4Headers);
        try {
            Data r = this.reader4Headers.read(null, this.binaryDecoder4Headers);
            return DataContainer.createDataContainer4Read(null, r);//TODO get meta
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void configure(Map<String, ?> map, boolean b) {

    }


    @Override
    public void close() {

    }
}