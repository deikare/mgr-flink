package vfdt.classifiers.dwm.extended;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.base.BaseDynamicWeightedMajority;
import vfdt.classifiers.dwm.DwmClassifierFields;
import vfdt.classifiers.dwm.classic.ClassifierInterface;
import vfdt.classifiers.helpers.Helpers;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;

import static vfdt.classifiers.helpers.Helpers.getIndexOfHighestValue;

public abstract class ExtendedDynamicWeightedMajority<C extends ClassifierInterface> extends BaseDynamicWeightedMajority<C, ClassifierPojoExtended<C>> {
    protected boolean anyWeightChanged; //two ideas - first to use flag, second to use array of lowered weight classifiers indices - second idea doesn't work because still all classifiers should be normalized

    public ExtendedDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        super(beta, threshold, classNumber, updateClassifiersEachSamples);
        anyWeightChanged = false;
    }

    @Override
    protected ClassifierPojoExtended<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojoExtended<>(createClassifier(), sampleNumber);
    }

    @Override
    protected ArrayList<Tuple2<String, Long>> trainImplementation(Example example, int predictedClass, ArrayList<Tuple2<String, Long>> performances) {
        int actualClass = example.getMappedClass();
        if (anyWeightChanged) {
            anyWeightChanged = false;
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
            ClassifierPojoExtended<C> classifierAndWeight = classifiersPojo.get(classifierIndex);
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

        int actualClass = example.getMappedClass();

        Double[] votesForEachClass = initializeVoteForEachClass();

        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            ClassifierPojoExtended<C> classifierAndWeight = classifiersPojo.get(classifierIndex);

            Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResults = classifierAndWeight.classify(example);

            updateGlobalWithLocalPerformances(classifyResults.f1, globalClassifyResults);

            if (classifyResults.f0 != actualClass) {
                classifierAndWeight.incWrongClassificationCounter();
                if (classifierAndWeight.getWrongClassificationsCounter() % updateClassifiersEachSamples == 0) {
                    classifierAndWeight.clearWrongClassificationCounter();
                    classifierAndWeight.lowerWeight(beta);
                    anyWeightChanged = true;
                }
            }

            votesForEachClass[classifyResults.f0] += classifierAndWeight.getWeight();

            classifiersPojo.set(classifierIndex, classifierAndWeight);
        }

        averagePerformanceByLocalClassifier(globalClassifyResults, classifiersPojo.size());

        return Tuple2.of(getIndexOfHighestValue(votesForEachClass), globalClassifyResults);
    }
}
