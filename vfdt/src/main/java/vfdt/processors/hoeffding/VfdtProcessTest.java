package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.inputs.Example;

public class VfdtProcessTest extends KeyedProcessFunction<Long, Example, String> {
    ValueState<Long> counter;

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        Long count = counter.value();
        if (count == null)
            count = 0L;
        count++;
        count += example.getId();
        collector.collect(count.toString());
        counter.update(count);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        System.out.println(counter);

        counter = getRuntimeContext().getState(new ValueStateDescriptor<>("counter", Types.LONG));
    }
}
