package vfdt.classifiers.dwm.classic;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.base.BaseClassifierClassifyAndTrain;
import vfdt.classifiers.dwm.DwmClassifierFields;
import vfdt.classifiers.helpers.Helpers;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.*;

import static vfdt.classifiers.helpers.Helpers.getIndexOfHighestValue;

public abstract class DynamicWeightedMajority<C extends ClassifierInterface> extends BaseClassifierClassifyAndTrain {
    //todo try to only train new classifiers on n new samples, without classification
    protected final double beta;
    protected final double threshold;
    protected final int classNumber;
    protected int sampleNumber;
    protected final int updateClassifiersEachSamples;

    protected ArrayList<Tuple2<C, Double>> classifiersWithWeights;

    protected DynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        this.beta = beta;
        this.threshold = threshold;
        this.classNumber = classNumber;
        this.updateClassifiersEachSamples = updateClassifiersEachSamples;
        this.sampleNumber = 0;
        this.classifiersWithWeights = new ArrayList<>(Collections.singletonList(createClassifierWithWeight()));
    }

    @Override
    protected ArrayList<Tuple2<String, Long>> trainImplementation(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances) {
        if (sampleNumber % updateClassifiersEachSamples == 0) {
            ArrayList<Tuple2<String, Long>> normalizationAndDeletePerformances = normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold();
            if (predictedClass != example.getMappedClass()) {
                Instant start = Instant.now();
                classifiersWithWeights.add(createClassifierWithWeight(example));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADD_CLASSIFIER_DURATION, Helpers.toNow(start)));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADDED_CLASSIFIERS_COUNT, 1L));
            }
            performances.addAll(normalizationAndDeletePerformances);
        }
        return performances;
    }

    protected ArrayList<Tuple2<String, Long>> normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold() {
        ArrayList<Tuple2<String, Long>> performances = new ArrayList<>(2);

        double weightsSum = classifiersWithWeights.stream().mapToDouble(classifierAndWeight -> classifierAndWeight.f1).sum();
        ListIterator<Tuple2<C, Double>> classifierIterator = classifiersWithWeights.listIterator();
        long deletedCount = 0;

        Instant start = Instant.now();

        while (classifierIterator.hasNext()) {
            Tuple2<C, Double> classifierAndWeight = classifierIterator.next();
            classifierAndWeight.f1 /= weightsSum;
            if (classifierAndWeight.f1 < threshold) {
                classifierIterator.remove(); // todo think - when delete it imbalances weight normalization - probably not, because voting ratio stays the same
                deletedCount++;
            } else classifierIterator.set(classifierAndWeight);
        }

        performances.add(Tuple2.of(DwmClassifierFields.WEIGHTS_NORMALIZATION_AND_CLASSIFIER_DELETE_DURATION, Helpers.toNow(start)));
        performances.add(Tuple2.of(DwmClassifierFields.DELETED_CLASSIFIERS_COUNT, deletedCount));

        return performances;
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example) {
        sampleNumber++;

        Double[] votesForEachClass = initializeVoteForEachClass();
        ArrayList<Tuple2<String, Long>> globalClassifyResults = new ArrayList<>();
        ListIterator<Tuple2<C, Double>> classifiersIterator = classifiersWithWeights.listIterator();

        while (classifiersIterator.hasNext()) {
            Tuple2<C, Double> classifierAndWeight = classifiersIterator.next();
            Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResults = classifierAndWeight.f0.classify(example);
            ArrayList<Tuple2<String, Long>> classifyMeasurements = classifyResults.f1;

            for (int i = 0; i < classifyMeasurements.size(); i++) {
                if (i >= globalClassifyResults.size())
                    globalClassifyResults.add(classifyMeasurements.get(i));
                else {
                    Tuple2<String, Long> measurementFromGlobal = globalClassifyResults.get(i);
                    measurementFromGlobal.f1 += classifyMeasurements.get(i).f1;
                    globalClassifyResults.set(i, measurementFromGlobal);
                }

                int classNumber = classifyResults.f0;
                if (classNumber != example.getMappedClass() && sampleNumber % updateClassifiersEachSamples == 0)
                    classifierAndWeight.f1 *= beta;
                votesForEachClass[classNumber] += classifierAndWeight.f1;
                classifiersIterator.set(classifierAndWeight);
            }
        }

        int predicted = getIndexOfHighestValue(votesForEachClass);

        ListIterator<Tuple2<String, Long>> classifyResultsIterator = globalClassifyResults.listIterator();
        while (classifyResultsIterator.hasNext()) {
            Tuple2<String, Long> measurement = classifyResultsIterator.next();
            measurement.f1 /= classifiersWithWeights.size();
            classifyResultsIterator.set(measurement);
        }

        return Tuple2.of(predicted, globalClassifyResults);
    }

    protected Double[] initializeVoteForEachClass() {
        Double[] result = new Double[classNumber];
        for (int i = 0; i < classNumber; i++) {
            result[i] = 0.0;
        }

        return result;
    }

    protected Tuple2<C, Double> createClassifierWithWeight(Example example) {
        return Tuple2.of(createClassifier(example), 1.0);
    }

    protected abstract C createClassifier(Example example);
}
