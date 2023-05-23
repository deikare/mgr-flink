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
    protected DummyClassifier createClassifier() {
        return new DummyClassifier();
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<DummyClassifier> typeInformation = TypeInformation.of(new TypeHint<DummyClassifier>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classifier", typeInformation));
    }
}
