package com.github.fevernova.framework.service.state.storage;


import com.alibaba.fastjson.JSON;
import com.github.fevernova.framework.common.Util;
import com.github.fevernova.framework.common.context.GlobalContext;
import com.github.fevernova.framework.common.context.JobTags;
import com.github.fevernova.framework.common.context.TaskContext;
import com.github.fevernova.framework.common.data.BarrierData;
import com.github.fevernova.framework.component.ComponentType;
import com.github.fevernova.framework.service.state.AchieveClean;
import com.github.fevernova.framework.service.state.StateValue;
import com.github.fevernova.framework.service.uniq.WireToOutputStream2;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.wire.InputStreamToWire;
import net.openhft.chronicle.wire.Wire;
import net.openhft.chronicle.wire.WireType;
import org.apache.commons.lang3.Validate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


@Slf4j
public class FSStorage extends IStorage {


    private static final String TXT = ".txt";

    private static final String BIN = ".bin";

    private String statePath;

    private String dataPath;


    public FSStorage(GlobalContext globalContext, TaskContext taskContext) {

        super(globalContext, taskContext);
        String basePath = taskContext.getString("basepath", "/tmp/fevernova");
        JobTags tags = globalContext.getJobTags();
        String taskPath = "/" + tags.getJobType() + "-" + tags.getJobId() + "/" + tags.getPodTotalNum() + "-" + tags.getPodIndex() + "/";
        this.statePath = basePath + taskPath + "state/";
        this.dataPath = basePath + taskPath + "data/";
        Util.mkDir(new File(this.statePath));
        Util.mkDir(new File(this.dataPath));
    }


    @Override public void saveStateValue(BarrierData barrierData, List<StateValue> stateValueList) {

        String stateFilePath = this.statePath + barrier2FileName(barrierData, TXT);
        log.info("FSStorage saveStateValue : " + stateFilePath);
        Util.writeFile(stateFilePath, JSON.toJSONBytes(stateValueList));
    }


    @Override public void achieveStateValue(BarrierData barrierData, AchieveClean achieveClean) {

        switch (achieveClean) {
            case CURRENT:
                File file = new File(this.statePath + barrier2FileName(barrierData, TXT));
                if (file.exists()) {
                    log.info("FSStorage delete : " + file.getName());
                    Validate.isTrue(file.delete());
                }
                return;
            case ALL:
                File[] allFiles = new File(this.statePath).listFiles(pathname -> pathname.getName().endsWith(TXT));
                if (allFiles != null && allFiles.length > 0) {
                    for (File filea : allFiles) {
                        log.info("FSStorage delete : " + filea.getName());
                        Validate.isTrue(filea.delete());
                    }
                }
                return;
            case BEFORE:
                String current = barrier2FileName(barrierData, TXT);
                File[] files = new File(this.statePath).listFiles(pathname -> pathname.getName().endsWith(TXT));
                if (files != null && files.length > 0) {
                    for (File fileb : files) {
                        if (current.compareTo(fileb.getName()) > 0) {
                            log.info("FSStorage delete : " + fileb.getName());
                            Validate.isTrue(fileb.delete());
                        }
                    }
                }
                return;
        }
    }


    @Override public List<StateValue> recoveryStateValue() {

        File[] files = new File(this.statePath).listFiles(pathname -> pathname.getName().endsWith(TXT));
        if (files != null && files.length > 0) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            File file = files[files.length - 1];
            String data = Util.readFile(file);
            List<StateValue> result = JSON.parseArray(data, StateValue.class);
            return result;
        }
        return Lists.newArrayList();
    }


    @Override public String saveBinary(ComponentType componentType, int total, int index, BarrierData barrierData, WriteBytesMarshallable obj) {

        String stateFilePath = this.dataPath + component2FileName(componentType, total, index) + "_" + barrier2FileName(barrierData, BIN);
        log.info("FSStorage saveBinary : " + stateFilePath);
        final Path path = Paths.get(stateFilePath);
        log.info("Writing state to {} ...", path);
        try (final OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW);
                final OutputStream bos = new BufferedOutputStream(os);
                final WireToOutputStream2 wireToOutputStream = new WireToOutputStream2(WireType.RAW, bos)) {

            final Wire wire = wireToOutputStream.getWire();
            wire.writeBytes(obj);
            log.info("done serializing, flushing {} ...", path);
            wireToOutputStream.flush();
            //bos.flush();
            log.info("completed {}", path);
        } catch (final IOException ex) {
            log.error("Can not write snapshot file: ", ex);
            Validate.isTrue(false);
        }
        achieveBinary(component2FileName(componentType, total, index), 10);
        return stateFilePath;
    }


    private void achieveBinary(String pre, int remain) {

        File[] files = new File(this.dataPath).listFiles(pathname -> pathname.getName().startsWith(pre) && pathname.getName().endsWith(BIN));
        if (files != null && files.length > remain) {
            Arrays.sort(files, Comparator.comparing(File::getName));
            for (int i = 0; i < files.length - remain; i++) {
                log.info("FSStorage delete : " + files[i].getName());
                Validate.isTrue(files[i].delete());
            }
        }
    }


    @Override public void recoveryBinary(String stateFilePath, ReadBytesMarshallable obj) {

        final Path path = Paths.get(stateFilePath);
        log.info("FSStorage loadbinary from {}", path);
        try (final InputStream is = Files.newInputStream(path, StandardOpenOption.READ);
                final InputStream bis = new BufferedInputStream(is)) {

            final InputStreamToWire inputStreamToWire = new InputStreamToWire(WireType.RAW, bis);
            final Wire wire = inputStreamToWire.readOne();
            log.info("start de-serializing...");
            wire.readBytes(obj);
        } catch (final IOException ex) {
            log.error("Can not read snapshot file: ", ex);
            Validate.isTrue(false);
        }
    }


    private String barrier2FileName(BarrierData barrierData, String end) {

        return barrierData.getTimestamp() + "_" + barrierData.getBarrierId() + end;
    }


    private String component2FileName(ComponentType componentType, int total, int index) {

        return componentType.name() + "_" + total + "_" + index;
    }
}
