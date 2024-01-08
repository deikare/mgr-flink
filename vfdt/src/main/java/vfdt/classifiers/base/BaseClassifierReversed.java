package vfdt.classifiers.base;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import vfdt.inputs.Example;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;

public abstract class BaseClassifierReversed extends BaseClassifier implements Serializable {
    public HashMap<String, Long> train(Example example, HashMap<String, Long> performances) {
        Instant start = Instant.now();
        HashMap<String, Long> trainingPerformance = trainImplementation(example, performances);
        trainingPerformance.put(BaseClassifierFields.TRAINING_DURATION, toNow(start));
        return trainingPerformance;
    }

    public Tuple3<String, String, HashMap<String, Long>> classify(Example example) {
        Instant start = Instant.now();
        Tuple2<String, HashMap<String, Long>> predictionResult = classifyImplementation(example);
        predictionResult.f1.put(BaseClassifierFields.CLASSIFICATION_DURATION, toNow(start));
        return new Tuple3<>(timestampTrailingZeros(start), predictionResult.f0, predictionResult.f1);
    }

    protected abstract HashMap<String, Long> trainImplementation(Example example, HashMap<String, Long> performances);

    protected abstract Tuple2<String, HashMap<String, Long>> classifyImplementation(Example example);
}
