package vfdt;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.hoeffding.BaseClassifier;
import vfdt.hoeffding.BaseClassifierFields;
import vfdt.hoeffding.Example;

import java.util.HashMap;

public class DummyClassifier extends BaseClassifier {
    @Override
    protected HashMap<String, Long> trainImplementation(Example example) {
        return new HashMap<>();
    }

    @Override
    protected Tuple2<String, HashMap<String, Long>> classifyImplementation(Example example, HashMap<String, Long> performances) {
        HashMap<String, Long> result = new HashMap<>();
        result.put(BaseClassifierFields.CLASSIFICATION_DURATION, 0L);
        String text = "test";
        return Tuple2.of(text, result);
    }

    @Override
    public String generateClassifierParams() {
        return "test";
    }
}
