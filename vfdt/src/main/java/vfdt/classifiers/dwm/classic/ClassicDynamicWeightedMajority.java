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
        if (sampleNumber % updateClassifiersEachSamples == 0) {
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
            //czyli trzeba jak obcinamy wagę, to sortować w odpowiedniej kolejności klasyfikator
            //w ten sposób max będzie zawsze z przodu po przejściu wszystkich
            ClassifierPojo<C> classifierAndWeight = classifiersPojo.get(classifierIndex);
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
    protected long lowerWeightAndReturnWeightLoweringCount(ClassifierPojo<C> classifierPojo, long weightsLoweringCount) {
        if (sampleNumber % updateClassifiersEachSamples == 0) {
            weightsLoweringCount++;
            classifierPojo.lowerWeight(beta);
        }
        return weightsLoweringCount;
    }

    @Override
    protected ClassifierPojo<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojo<>(createClassifier(), sampleNumber);
    }
}
