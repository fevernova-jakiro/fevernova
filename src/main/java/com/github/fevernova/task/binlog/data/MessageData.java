package com.github.fevernova.task.binlog.data;


import com.github.fevernova.framework.common.data.Data;
import com.github.fevernova.io.data.message.DataContainer;
import lombok.Getter;
import lombok.Setter;


@Setter
@Getter
public class MessageData implements Data {


    private String destTopic;

    private byte[] key;

    private DataContainer dataContainer;

    private long timestamp;


    @Override public void clearData() {

        this.destTopic = null;
        this.key = null;
        this.dataContainer = null;
        this.timestamp = 0;
    }


    @Override public byte[] getBytes() {

        return null;
    }

}
