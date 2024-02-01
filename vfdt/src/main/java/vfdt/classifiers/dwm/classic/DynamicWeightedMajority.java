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
    //todo try to first train then classify
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
    public void bootstrapTrainImplementation(Example example) {
        for (int classifierIndex = 0; classifierIndex < classifiersWithWeights.size(); classifierIndex++) {
            Tuple2<C, Double> classifierAndWeight = classifiersWithWeights.get(classifierIndex);
            classifierAndWeight.f0.train(example);
            classifiersWithWeights.set(classifierIndex, classifierAndWeight);
        }
    }

    @Override
    protected ArrayList<Tuple2<String, Long>> trainImplementation(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances) {
        if (sampleNumber % updateClassifiersEachSamples == 0) {
            ArrayList<Tuple2<String, Long>> normalizationAndDeletePerformances = normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold();
            if (predictedClass != example.getMappedClass()) {
                Instant start = Instant.now();
                classifiersWithWeights.add(createClassifierWithWeight());
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADD_CLASSIFIER_DURATION, Helpers.toNow(start)));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADDED_CLASSIFIERS_COUNT, 1L));
            }
            performances.addAll(normalizationAndDeletePerformances);
        }

        for (int classifierIndex = 0; classifierIndex < classifiersWithWeights.size(); classifierIndex++) {
            Tuple2<C, Double> classifierAndWeight = classifiersWithWeights.get(classifierIndex);
            ArrayList<Tuple2<String, Long>> localClassifierPerformances = classifierAndWeight.f0.train(example);

            updateGlobalWithLocalPerformances(localClassifierPerformances, performances);

            classifiersWithWeights.set(classifierIndex, classifierAndWeight);
        }

        averagePerformanceByLocalClassifier(performances, classifiersWithWeights.size());

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
                classifierIterator.remove();
                deletedCount++;
            } else classifierIterator.set(classifierAndWeight);
        }

        performances.add(Tuple2.of(DwmClassifierFields.WEIGHTS_NORMALIZATION_AND_CLASSIFIER_DELETE_DURATION, Helpers.toNow(start)));
        performances.add(Tuple2.of(DwmClassifierFields.DELETED_CLASSIFIERS_COUNT, deletedCount));

        return performances;
    }

    @Override
    public String generateClassifierParams() {
        return "b" + beta + "_t" + threshold + "_u" + updateClassifiersEachSamples;
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example) {
        sampleNumber++;
        ArrayList<Tuple2<String, Long>> globalClassifyResults = new ArrayList<>();

        int predicted;

        int usedClassifiersCount = 0;
        Double[] votesForEachClass = initializeVoteForEachClass();

        for (int classifierIndex = 0; classifierIndex < classifiersWithWeights.size(); classifierIndex++) {
            Tuple2<C, Double> classifierAndWeight = classifiersWithWeights.get(classifierIndex);
            C classifier = classifierAndWeight.f0;
            if (classifier.getSampleNumber() != 0) {
                usedClassifiersCount++;
                Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResults = classifier.classify(example);
                ArrayList<Tuple2<String, Long>> classifyMeasurements = classifyResults.f1;

                updateGlobalWithLocalPerformances(classifyMeasurements, globalClassifyResults);

                updateWeightsAndVotes(example, classifyResults.f0, classifierAndWeight, votesForEachClass);

                classifiersWithWeights.set(classifierIndex, classifierAndWeight);
            }
        }

        predicted = getIndexOfHighestValue(votesForEachClass);

        averagePerformanceByLocalClassifier(globalClassifyResults, usedClassifiersCount);

        globalClassifyResults.add(Tuple2.of(DwmClassifierFields.USED_CLASSIFIERS_AMOUNT_IN_CLASSIFICATION, Integer.toUnsignedLong(usedClassifiersCount)));

        return Tuple2.of(predicted, globalClassifyResults);
    }

    private static void averagePerformanceByLocalClassifier(ArrayList<Tuple2<String, Long>> globalClassifyResults, int usedClassifiersCount) {
        for (int resultIndex = 0; resultIndex < globalClassifyResults.size(); resultIndex++) {
            Tuple2<String, Long> measurement = globalClassifyResults.get(resultIndex);
            measurement.f1 /= usedClassifiersCount;
            globalClassifyResults.set(resultIndex, measurement);
        }
    }

    private void updateWeightsAndVotes(Example example, int classNumber, Tuple2<C, Double> classifierAndWeight, Double[] votesForEachClass) {
        if (classNumber != example.getMappedClass() && sampleNumber % updateClassifiersEachSamples == 0)
            classifierAndWeight.f1 *= beta;
        votesForEachClass[classNumber] += classifierAndWeight.f1;
    }

    protected static void updateGlobalWithLocalPerformances(ArrayList<Tuple2<String, Long>> performances, ArrayList<Tuple2<String, Long>> globalClassifyResults) {
        for (int localMeasurementIndex = 0; localMeasurementIndex < performances.size(); localMeasurementIndex++) {
            if (localMeasurementIndex >= globalClassifyResults.size())
                globalClassifyResults.add(performances.get(localMeasurementIndex));
            else {
                Tuple2<String, Long> measurementFromGlobal = globalClassifyResults.get(localMeasurementIndex);
                measurementFromGlobal.f1 += performances.get(localMeasurementIndex).f1;
                globalClassifyResults.set(localMeasurementIndex, measurementFromGlobal);
            }
        }
    }

    protected Double[] initializeVoteForEachClass() {
        Double[] result = new Double[classNumber];
        for (int i = 0; i < classNumber; i++) {
            result[i] = 0.0;
        }

        return result;
    }

    protected Tuple2<C, Double> createClassifierWithWeight() {
        return Tuple2.of(createClassifier(), 1.0);
    }

    protected abstract C createClassifier();
}
