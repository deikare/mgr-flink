package vfdt.classifiers.dwm.classifiers.bayes.naive;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.dwm.classic.ClassifierInterface;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static vfdt.classifiers.helpers.Helpers.toNow;

public class NaiveBayesClassifier implements ClassifierInterface {
    private long nSamples;

    ArrayList<Tuple2<Long, ArrayList<HashMap<Double, Long>>>> classAndAttributeCountsForEachClass;

    public NaiveBayesClassifier(int classNumber, Example firstExample) {
        nSamples = 1L;
        classAndAttributeCountsForEachClass = new ArrayList<>(classNumber);
        double[] attributes = firstExample.getAttributes();
        int exampleClass = firstExample.getMappedClass();

        initClassAndAttributeCounts(0, exampleClass, attributes.length);

        ArrayList<HashMap<Double, Long>> attributeCountsList = new ArrayList<>(attributes.length);
        for (double attributeValue : attributes) {
            HashMap<Double, Long> attributesMap = new HashMap<>(1);
            attributesMap.put(attributeValue, 1L);
            attributeCountsList.add(attributesMap);
        }
        classAndAttributeCountsForEachClass.add(Tuple2.of(1L, attributeCountsList));

        initClassAndAttributeCounts(exampleClass + 1, classNumber, attributes.length);
    }

    private void initClassAndAttributeCounts(int classBeginningIndex, int classEndIndex, int attributesNumber) {
        for (int i = classBeginningIndex; i < classEndIndex; i++) {
            ArrayList<HashMap<Double, Long>> attributeCountsForEachAttribute = new ArrayList<>(attributesNumber);
            for (int j = 0; j < attributesNumber; j++) {
                attributeCountsForEachAttribute.add(new HashMap<>());
            }
            classAndAttributeCountsForEachClass.add(Tuple2.of(0L, attributeCountsForEachAttribute));
        }
    }

    @Override
    public Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classify(Example example) {
        Instant start = Instant.now();

        nSamples++;

        int predictedClass = 0;
        double maxProbability = 0.0;

        double[] attributes = example.getAttributes();

        for (int classNumber = 0; classNumber < classAndAttributeCountsForEachClass.size(); classNumber++) {
            Tuple2<Long, ArrayList<HashMap<Double, Long>>> classCountAndAttributesCounts = classAndAttributeCountsForEachClass.get(classNumber);
            double classProbability = ((double) classCountAndAttributesCounts.f0) / ((double) (nSamples));

            double multipliedProbability = 1.0;
            for (int attributeNumber = 0; attributeNumber < attributes.length; attributeNumber++) {
                HashMap<Double, Long> attributeCounts = classCountAndAttributesCounts.f1.get(attributeNumber);
                double totalCountOfAttributeValues = (double) (attributeCounts.values().stream().mapToLong(Long::longValue).sum());
                double attributeValueProbability = ((double) attributeCounts.get(attributes[attributeNumber])) / (totalCountOfAttributeValues);
                multipliedProbability *= attributeValueProbability;
            }

            double probability = classProbability * multipliedProbability;
            if (probability > maxProbability) {
                predictedClass = classNumber;
                maxProbability = probability;
            }
        }

        return Tuple2.of(predictedClass, new ArrayList<>(Collections.singletonList(Tuple2.of(NaiveBayesFields.AVG_CLASSIFY_DURATION, toNow(start)))));
    }

    @Override
    public ArrayList<Tuple2<String, Long>> train(Example example) {
        Instant start = Instant.now();
        int exampleClass = example.getMappedClass();
        Tuple2<Long, ArrayList<HashMap<Double, Long>>> classAndAttributeCounts = classAndAttributeCountsForEachClass.get(exampleClass);
        classAndAttributeCounts.f0++;
        double[] attributes = example.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            HashMap<Double, Long> attributeValueCounts = classAndAttributeCounts.f1.get(i);

            attributeValueCounts.merge(attributes[i], 1L, Long::sum);
            classAndAttributeCounts.f1.set(i, attributeValueCounts);
        }

        classAndAttributeCountsForEachClass.set(exampleClass, classAndAttributeCounts);
        return new ArrayList<>(Collections.singletonList(Tuple2.of(NaiveBayesFields.AVG_TRAIN_DURATION, toNow(start))));
    }
}
