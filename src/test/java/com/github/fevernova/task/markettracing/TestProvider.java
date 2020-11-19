package com.github.fevernova.task.markettracing;


import com.github.fevernova.framework.component.DataProvider;
import com.github.fevernova.task.markettracing.data.TriggerResult;
import com.github.fevernova.task.markettracing.data.TriggerResultFactory;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;


@Slf4j
public class TestProvider implements DataProvider<Integer, TriggerResult> {


    protected TriggerResult triggerResult = (TriggerResult) new TriggerResultFactory().createData();

    private boolean flag = false;

    @Getter
    private int count = 0;

    @Setter
    private boolean print;


    public TestProvider(boolean print) {

        this.print = print;
    }


    @Override public TriggerResult feedOne(Integer key) {

        Validate.isTrue(!this.flag);
        this.flag = true;
        return this.triggerResult;
    }


    @Override public void push() {

        Validate.isTrue(this.flag);
        this.flag = false;
        this.count++;
        if (this.print) {
            log.info(this.triggerResult.toString());
        }
        this.triggerResult.clearData();
    }

}
