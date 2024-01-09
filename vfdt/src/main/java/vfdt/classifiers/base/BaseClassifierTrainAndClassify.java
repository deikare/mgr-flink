package vfdt.classifiers.base;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.HashMap;

public abstract class BaseClassifierTrainAndClassify extends BaseClassifier {
    public Tuple2<String, HashMap<String, Long>> train(Example example) {
        Instant start = Instant.now();
        HashMap<String, Long> trainingPerformance = trainImplementation(example);
        trainingPerformance.put(BaseClassifierFields.TRAINING_DURATION, toNow(start));
        return new Tuple2<>(timestampTrailingZeros(start), trainingPerformance);
    }

    public Tuple2<Integer, HashMap<String, Long>> classify(Example example, HashMap<String, Long> performances) {
        Instant start = Instant.now();
        Tuple2<Integer, HashMap<String, Long>> predictionResult = classifyImplementation(example, performances);
        predictionResult.f1.put(BaseClassifierFields.CLASSIFICATION_DURATION, toNow(start));
        return predictionResult;
    }

    protected abstract HashMap<String, Long> trainImplementation(Example example);

    protected abstract Tuple2<Integer, HashMap<String, Long>> classifyImplementation(Example example, HashMap<String, Long> performances);
}
