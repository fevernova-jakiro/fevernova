package com.github.fevernova.framework.window;


import com.github.fevernova.task.exchange.engine.SerializationUtils;
import net.openhft.chronicle.bytes.BytesIn;
import net.openhft.chronicle.bytes.BytesOut;
import net.openhft.chronicle.bytes.ReadBytesMarshallable;
import net.openhft.chronicle.bytes.WriteBytesMarshallable;
import net.openhft.chronicle.core.io.IORuntimeException;

import java.util.TreeMap;


public abstract class SlideWindow<W extends ObjectWithId> implements WriteBytesMarshallable, ReadBytesMarshallable {


    protected TreeMap<Integer, W> windows;

    protected long span;

    protected WindowListener<W> windowListener;

    protected W currentWindow;

    private int windowNum;

    //cache
    private int currentWindowSeq;


    public SlideWindow(long span, int windowNum) {

        this(span, windowNum, null);
    }


    public SlideWindow(long span, int windowNum, WindowListener windowListener) {

        this.span = span;
        this.windowNum = windowNum;
        this.windows = new TreeMap<>();
        this.windowListener = windowListener;
    }


    protected boolean prepareCurrentWindow(long timestamp) {

        int windowSeq = (int) (timestamp / this.span);
        if (this.currentWindowSeq == windowSeq) {
            return true;
        }

        this.currentWindowSeq = windowSeq;
        this.currentWindow = this.windows.get(this.currentWindowSeq);
        if (this.currentWindow != null) {
            return true;
        }

        if (this.windows.size() < this.windowNum) {
            this.currentWindow = newWindow(this.currentWindowSeq);
            this.windows.put(this.currentWindowSeq, this.currentWindow);
            if (this.windowListener != null) {
                this.windowListener.createNewWindow(this.currentWindow);
            }
            return true;
        }

        W w = this.windows.firstEntry().getValue();
        if (this.currentWindowSeq > w.getId()) {
            this.windows.remove(w.getId());
            if (this.windowListener != null) {
                this.windowListener.removeOldWindow(w);
            }
            this.currentWindow = newWindow(this.currentWindowSeq);
            this.windows.put(this.currentWindowSeq, this.currentWindow);
            if (this.windowListener != null) {
                this.windowListener.createNewWindow(this.currentWindow);
            }
            return true;
        }
        this.currentWindow = this.windows.lastEntry().getValue();
        this.currentWindowSeq = this.currentWindow.getId();
        return false;
    }


    protected abstract W newWindow(int seq);


    @Override public void writeMarshallable(BytesOut bytes) {

        SerializationUtils.writeIntMap(bytes, this.windows);
    }


    @Override public void readMarshallable(BytesIn bytes) throws IORuntimeException {

        int length = bytes.readInt();
        for (int i = 0; i < length; i++) {
            int k = bytes.readInt();
            W w = newWindow(k);
            w.readMarshallable(bytes);
            this.windows.put(k, w);
            if (this.windowListener != null) {
                this.windowListener.createNewWindow(w);
            }
        }
    }

}
