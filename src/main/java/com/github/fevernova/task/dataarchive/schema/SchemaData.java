package com.github.fevernova.task.dataarchive.schema;


import com.github.fevernova.framework.common.data.broadcast.BroadcastData;
import lombok.Getter;

import java.util.List;


@Getter
public class SchemaData extends BroadcastData {


    private List<ColumnInfo> columnInfos;


    public SchemaData(List<ColumnInfo> columnInfos) {

        this.columnInfos = columnInfos;
        super.align = false;
        super.global = true;
        super.onlyOnce = true;
    }


    @Override public byte[] getBytes() {

        return new byte[0];
    }


    @Override public long getTimestamp() {

        return 0;
    }
}
