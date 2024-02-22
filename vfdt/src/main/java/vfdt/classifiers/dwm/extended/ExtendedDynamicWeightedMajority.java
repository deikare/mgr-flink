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
        if (anyWeightChanged) {
            anyWeightChanged = false;
            ArrayList<Tuple2<String, Long>> normalizationAndDeletePerformances = normalizeWeightsAndDeleteClassifiersWithWeightUnderThreshold();
            if (predictedClass != example.getMappedClass()) {
                Instant start = Instant.now();
                classifiersPojo.add(createClassifierWithWeight(sampleNumber));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADD_CLASSIFIER_DURATION, Helpers.toNow(start)));
                normalizationAndDeletePerformances.add(Tuple2.of(DwmClassifierFields.ADDED_CLASSIFIERS_COUNT, 1L));
            }
            performances.addAll(normalizationAndDeletePerformances);
        }

        ArrayList<Tuple2<String, Long>> avgLocalPerformances = new ArrayList<>();
        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            ClassifierPojoExtended<C> classifierAndWeight = classifiersPojo.get(classifierIndex);
            ArrayList<Tuple2<String, Long>> localClassifierPerformances = classifierAndWeight.train(example);

            updateGlobalWithLocalPerformances(localClassifierPerformances, avgLocalPerformances);

            classifiersPojo.set(classifierIndex, classifierAndWeight);
        }

        averagePerformanceByLocalClassifier(avgLocalPerformances, classifiersPojo.size());
        performances.addAll(avgLocalPerformances);

        performances.add(Tuple2.of(DwmClassifierFields.CLASSIFIERS_AFTER_TRAIN_COUNT, (long) classifiersPojo.size()));

        return performances;
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example) {
        sampleNumber++;
        ArrayList<Tuple2<String, Long>> globalClassifyResults = new ArrayList<>();

        Double[] votesForEachClass = initializeVoteForEachClass();

        long weightsLoweringCount = 0L;

        long correctVotesCount = 0L;
        long wrongVotesCount = 0L;

        for (int classifierIndex = 0; classifierIndex < classifiersPojo.size(); classifierIndex++) {
            ClassifierPojoExtended<C> classifierAndWeight = classifiersPojo.get(classifierIndex);

            Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResults = classifierAndWeight.classify(example);

            updateGlobalWithLocalPerformances(classifyResults.f1, globalClassifyResults);

            if (classifyResults.f0 == example.getMappedClass())
                correctVotesCount++;
            else {
                wrongVotesCount++;
                classifierAndWeight.incWrongClassificationCounter();
                if (classifierAndWeight.getWrongClassificationsCounter() % updateClassifiersEachSamples == 0) {
                    classifierAndWeight.clearWrongClassificationCounter();
                    classifierAndWeight.lowerWeight(beta);
                    anyWeightChanged = true;
                    weightsLoweringCount++;
                }
            }

            votesForEachClass[classifyResults.f0] += classifierAndWeight.getWeight();

            classifiersPojo.set(classifierIndex, classifierAndWeight);
        }

        averagePerformanceByLocalClassifier(globalClassifyResults, classifiersPojo.size());

        globalClassifyResults.add(Tuple2.of(DwmClassifierFields.WEIGHTS_LOWERING_COUNT, weightsLoweringCount));
        globalClassifyResults.add(Tuple2.of(DwmClassifierFields.CORRECT_VOTES_COUNT, correctVotesCount));
        globalClassifyResults.add(Tuple2.of(DwmClassifierFields.WRONG_VOTES_COUNT, wrongVotesCount));

        return Tuple2.of(getIndexOfHighestValue(votesForEachClass), globalClassifyResults);
    }
}
