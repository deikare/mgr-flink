package vfdt.classifiers.base;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import vfdt.inputs.Example;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;

public abstract class BaseClassifierClassifyAndTrain extends BaseClassifier implements Serializable {
    public ArrayList<Tuple2<String, Long>> train(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances) {
        Instant start = Instant.now();
        ArrayList<Tuple2<String, Long>> trainingPerformance = trainImplementation(example, predictedClass, performances);
        trainingPerformance.add(Tuple2.of(BaseClassifierFields.TRAINING_DURATION, toNow(start)));
        return trainingPerformance;
    }

    public Tuple3<String, Integer, ArrayList<Tuple2<String, Long>>> classify(Example example) {
        Instant start = Instant.now();
        Tuple2<Integer, ArrayList<Tuple2<String, Long>>> predictionResult = classifyImplementation(example);
        predictionResult.f1.add(Tuple2.of(BaseClassifierFields.CLASSIFICATION_DURATION, toNow(start)));
        return new Tuple3<>(timestampTrailingZeros(start), predictionResult.f0, predictionResult.f1);
    }

    protected abstract ArrayList<Tuple2<String, Long>> trainImplementation(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances);

    protected abstract Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example);
}
