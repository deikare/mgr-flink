package vfdt.classifiers.base;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;

public abstract class BaseClassifierTrainAndClassify extends BaseClassifier {
    public Tuple2<String, ArrayList<Tuple2<String, Long>>> train(Example example) {
        Instant start = Instant.now();
        ArrayList<Tuple2<String, Long>> trainingPerformance = trainImplementation(example);
        trainingPerformance.add(Tuple2.of(BaseClassifierFields.TRAINING_DURATION, toNow(start)));
        return new Tuple2<>(timestampTrailingZeros(start), trainingPerformance);
    }

    public Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classify(Example example, ArrayList<Tuple2<String, Long>> performances) {
        Instant start = Instant.now();
        Tuple2<Integer, ArrayList<Tuple2<String, Long>>> predictionResult = classifyImplementation(example, performances);
        predictionResult.f1.add(Tuple2.of(BaseClassifierFields.CLASSIFICATION_DURATION, toNow(start)));
        return predictionResult;
    }

    protected abstract ArrayList<Tuple2<String, Long>> trainImplementation(Example example);

    protected abstract Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example, ArrayList<Tuple2<String, Long>> performances);
}
