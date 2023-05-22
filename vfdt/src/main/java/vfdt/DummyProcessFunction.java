package vfdt;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;

public class DummyProcessFunction extends BaseProcessFunction<DummyClassifier> {
    public DummyProcessFunction(String name, String dataset) {
        super(name, dataset);
    }

    @Override
    protected DummyClassifier getClassifier() {
        return new DummyClassifier();
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        TypeInformation<DummyClassifier> typeInformation = TypeInformation.of(new TypeHint<DummyClassifier>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classifier", typeInformation));
    }
}
