package vfdt;

public class DummyProcessFunction extends BaseProcessFunction<DummyClassifier> {
    @Override
    protected DummyClassifier getClassifier() {
        return new DummyClassifier();
    }
}
