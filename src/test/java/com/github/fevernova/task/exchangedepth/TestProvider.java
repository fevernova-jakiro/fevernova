package com.github.fevernova.task.exchangedepth;


import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.exchangedepth.data.DepthResult;
import com.github.fevernova.task.exchangedepth.data.DepthResultFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;


@Slf4j
public class TestProvider implements DataProvider<Integer, DepthResult> {


    protected DepthResult depthResult = (DepthResult) new DepthResultFactory().createData();

    private boolean flag = false;

    @Getter
    private int count = 0;

    @Setter
    private boolean print;


    public TestProvider(boolean print) {

        this.print = print;
    }


    @Override public DepthResult feedOne(Integer key) {

        Validate.isTrue(!this.flag);
        this.flag = true;
        return this.depthResult;
    }


    @Override public void push() {

        Validate.isTrue(this.flag);
        this.flag = false;
        this.count++;
        if (this.print) {
            log.info(this.depthResult.toString());
        }
        this.depthResult.clearData();
    }

}
