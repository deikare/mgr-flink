package vfdt.classifiers.base;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.dwm.DwmClassifierFields;
import vfdt.classifiers.dwm.classic.ClassifierInterface;
import vfdt.classifiers.helpers.Helpers;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

public abstract class BaseDynamicWeightedMajority<C extends ClassifierInterface, T extends ClassifierPojo<C>> extends BaseClassifierClassifyAndTrain {
    protected final double beta;
    protected final double threshold;
    protected final int classNumber;
    protected long sampleNumber;
    protected final int updateClassifiersEachSamples;

    protected ArrayList<T> classifiersPojo;

    public BaseDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        this.beta = beta;
        this.threshold = threshold;
        this.classNumber = classNumber;
        this.updateClassifiersEachSamples = updateClassifiersEachSamples;
        this.sampleNumber = 0;
        this.classifiersPojo = new ArrayList<>(Collections.singletonList(createClassifierWithWeight(1)));
    }

    @Override
    public void bootstrapTrainImplementation(Example example) {
        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            T classifier = classifiersPojo.get(classifierIndex);
            classifier.getClassifier().train(example);
            classifiersPojo.set(classifierIndex, classifier);
        }
    }

    protected ArrayList<Tuple2<String, Long>> normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold() {
        ArrayList<Tuple2<String, Long>> performances = new ArrayList<>(2);

        double weightsSum = classifiersPojo.stream().mapToDouble(ClassifierPojo::getWeight).sum();
        ListIterator<T> classifierIterator = classifiersPojo.listIterator();
        long deletedCount = 0;
        long deletedTTL = 0;

        Instant start = Instant.now();

        while (classifierIterator.hasNext()) {
            T classifierAndWeight = classifierIterator.next();
            classifierAndWeight.normalizeWeight(weightsSum);
            if (classifierAndWeight.getWeight() < threshold) {
                classifierIterator.remove();
                deletedCount++;
                deletedTTL += sampleNumber - classifierAndWeight.getSampleNumber();
            } else classifierIterator.set(classifierAndWeight);
        }

        performances.add(Tuple2.of(DwmClassifierFields.WEIGHTS_NORMALIZATION_AND_CLASSIFIER_DELETE_DURATION, Helpers.toNow(start)));
        performances.add(Tuple2.of(DwmClassifierFields.DELETED_CLASSIFIERS_COUNT, deletedCount));

        if (deletedCount != 0)
            performances.add(Tuple2.of(DwmClassifierFields.AVG_CLASSIFIER_TTL, deletedTTL / deletedCount));

        return performances;
    }

    protected static void averagePerformanceByLocalClassifier(ArrayList<Tuple2<String, Long>> globalClassifyResults, int usedClassifiersCount) {
        for (int resultIndex = 0; resultIndex < globalClassifyResults.size(); resultIndex++) {
            Tuple2<String, Long> measurement = globalClassifyResults.get(resultIndex);
            measurement.f1 /= usedClassifiersCount;
            globalClassifyResults.set(resultIndex, measurement);
        }
    }

    protected static void updateGlobalWithLocalPerformances(ArrayList<Tuple2<String, Long>> localPerformances, ArrayList<Tuple2<String, Long>> globalPerformances) {
        for (int localMeasurementIndex = 0; localMeasurementIndex < localPerformances.size(); localMeasurementIndex++) {
            if (localMeasurementIndex >= globalPerformances.size())
                globalPerformances.add(localPerformances.get(localMeasurementIndex));
            else {
                Tuple2<String, Long> measurementFromGlobal = globalPerformances.get(localMeasurementIndex);
                measurementFromGlobal.f1 += localPerformances.get(localMeasurementIndex).f1;
                globalPerformances.set(localMeasurementIndex, measurementFromGlobal);
            }
        }
    }

    @Override
    public String generateClassifierParams() {
        return "b" + beta + "_t" + threshold + "_u" + updateClassifiersEachSamples;
    }

    protected Double[] initializeVoteForEachClass() {
        Double[] result = new Double[classNumber];
        for (int i = 0; i < classNumber; i++) {
            result[i] = 0.0;
        }

        return result;
    }

    protected abstract C createClassifier();

    protected abstract T createClassifierWithWeight(long sampleNumber);
}
