package vfdt.classifiers.dwm.classic;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.base.BaseDynamicWeightedMajority;
import vfdt.classifiers.base.ClassifierPojo;
import vfdt.classifiers.dwm.DwmClassifierFields;
import vfdt.classifiers.helpers.Helpers;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;

import static vfdt.classifiers.helpers.Helpers.getIndexOfHighestValue;

public abstract class ClassicDynamicWeightedMajority<C extends ClassifierInterface> extends BaseDynamicWeightedMajority<C, ClassifierPojo<C>> {

    protected ClassicDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        super(beta, threshold, classNumber, updateClassifiersEachSamples);
    }

    @Override
    protected ArrayList<Tuple2<String, Long>> trainImplementation(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances) {
        int actualClass = example.getMappedClass();
        if (sampleNumber % updateClassifiersEachSamples == 0) {
            sampleNumber = 0L;
            ArrayList<Tuple2<String, Long>> normalizationAndDeletePerformances = normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold();
            if (predictedClass != actualClass) {
                Instant start = Instant.now();
                classifiersPojo.add(createClassifierWithWeight(sampleNumber));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADD_CLASSIFIER_DURATION, Helpers.toNow(start)));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADDED_CLASSIFIERS_COUNT, 1L));
            }
            performances.addAll(normalizationAndDeletePerformances);
        }

        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            ClassifierPojo<C> classifierAndWeight = classifiersPojo.get(classifierIndex);
            ArrayList<Tuple2<String, Long>> localClassifierPerformances = classifierAndWeight.train(example);

            updateGlobalWithLocalPerformances(localClassifierPerformances, performances);

            classifiersPojo.set(classifierIndex, classifierAndWeight);
        }

        averagePerformanceByLocalClassifier(performances, classifiersPojo.size());

        performances.add(Tuple2.of(DwmClassifierFields.CLASSIFIERS_AFTER_TRAIN_COUNT, (long) classifiersPojo.size()));

        return performances;
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example) {
        sampleNumber++;
        ArrayList<Tuple2<String, Long>> globalClassifyResults = new ArrayList<>();

        int predicted;
        int actualClass = example.getMappedClass();

        Double[] votesForEachClass = initializeVoteForEachClass();

        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            ClassifierPojo<C> classifierAndWeight = classifiersPojo.get(classifierIndex);

            Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResults = classifierAndWeight.classify(example);

            updateGlobalWithLocalPerformances(classifyResults.f1, globalClassifyResults);

            if (classifyResults.f0 != actualClass && sampleNumber % updateClassifiersEachSamples == 0)
                classifierAndWeight.lowerWeight(beta);

            votesForEachClass[classifyResults.f0] += classifierAndWeight.getWeight();

            classifiersPojo.set(classifierIndex, classifierAndWeight);
        }

        predicted = getIndexOfHighestValue(votesForEachClass);

        averagePerformanceByLocalClassifier(globalClassifyResults, classifiersPojo.size());

        return Tuple2.of(predicted, globalClassifyResults);
    }

    @Override
    protected ClassifierPojo<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojo<>(createClassifier(), sampleNumber);
    }
}
