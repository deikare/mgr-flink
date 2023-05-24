package vfdt.processors.dummies;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.processors.base.BaseProcessFunction;
import vfdt.classifiers.dummies.DummyClassifier;

public class DummyProcessFunction extends BaseProcessFunction<DummyClassifier> {
    public DummyProcessFunction() {
        super("", "");
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
