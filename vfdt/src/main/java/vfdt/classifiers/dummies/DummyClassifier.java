package vfdt.classifiers.dummies;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.base.BaseClassifier;
import vfdt.classifiers.base.BaseClassifierFields;
import vfdt.inputs.Example;

import java.util.HashMap;

public class DummyClassifier extends BaseClassifier {
    private String classifierFieldDummy = "dummy";

    @Override
    protected HashMap<String, Long> trainImplementation(Example example) {
        classifierFieldDummy.isEmpty();
        return new HashMap<>();
    }

    @Override
    protected Tuple2<String, HashMap<String, Long>> classifyImplementation(Example example, HashMap<String, Long> performances) {
        classifierFieldDummy.isEmpty();
        HashMap<String, Long> result = new HashMap<>();
        result.put(BaseClassifierFields.CLASSIFICATION_DURATION, 0L);
        return Tuple2.of(classifierFieldDummy, result);
    }

    @Override
    public String generateClassifierParams() {
        return "test";
    }
}
