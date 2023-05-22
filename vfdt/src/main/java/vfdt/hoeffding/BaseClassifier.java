package vfdt.hoeffding;

import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;

public abstract class BaseClassifier implements Serializable {
    public Tuple2<Long, HashMap<String, Long>> train(Example example) {
        Instant start = Instant.now();
        HashMap<String, Long> trainingPerformance = trainImplementation(example);
        trainingPerformance.put(BaseClassifierFields.TRAINING_DURATION, toNow(start));
        return new Tuple2<>(start.toEpochMilli(), trainingPerformance);
    }

    public Tuple2<String, HashMap<String, Long>> classify(Example example, HashMap<String, Long> performances) {
        Instant start = Instant.now();
        Tuple2<String, HashMap<String, Long>> predictionResult = classifyImplementation(example, performances);
        predictionResult.f1.put(BaseClassifierFields.CLASSIFICATION_DURATION, toNow(start));
        return predictionResult;
    }

    protected abstract HashMap<String, Long> trainImplementation(Example example);

    protected abstract Tuple2<String, HashMap<String, Long>> classifyImplementation(Example example, HashMap<String, Long> performances);


    public abstract String generateClassifierParams();

    protected long toNow(Instant start) {
        return Duration.between(start, Instant.now()).toMillis();
    }
}
