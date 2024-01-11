package vfdt.classifiers.dwm.classifiers.bayes.naive;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.dwm.classic.ClassifierInterface;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

import static vfdt.classifiers.helpers.Helpers.toNow;

public class GaussianNaiveBayesClassifier implements ClassifierInterface {
    private long sampleNumber;

    ArrayList<Tuple2<Long, ArrayList<Tuple2<Double, Double>>>> classCountAndAttributeSumsForEachClass;

    public GaussianNaiveBayesClassifier(int classesCount, int attributesCount) {
        sampleNumber = 0L;
        classCountAndAttributeSumsForEachClass = new ArrayList<>(classesCount);
        for (int classNumber = 0; classNumber < classesCount; classNumber++) {
            ArrayList<Tuple2<Double, Double>> attributeSums = new ArrayList<>(classesCount);
            for (int attributeNumber = 0; attributeNumber < attributesCount; attributeNumber++) {
                attributeSums.add(Tuple2.of(0.0, 0.0));
            }
            classCountAndAttributeSumsForEachClass.add(Tuple2.of(0L, attributeSums));
        }
    }

    @Override
    public long getSampleNumber() {
        return sampleNumber;
    }


    @Override
    public Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classify(Example example) {
        Instant start = Instant.now();

        sampleNumber++;

        int predictedClass = 0;
        double maxProbability = 0.0;

        double[] attributes = example.getAttributes();

        for (int classNumber = 0; classNumber < classCountAndAttributeSumsForEachClass.size(); classNumber++) {
            Tuple2<Long, ArrayList<Tuple2<Double, Double>>> classCountAndAttributesCounts = classCountAndAttributeSumsForEachClass.get(classNumber);
            double classProbability = ((double) classCountAndAttributesCounts.f0) / ((double) (sampleNumber));

            double multipliedProbability = 1.0;
            for (int attributeNumber = 0; attributeNumber < attributes.length; attributeNumber++) {
                double probability = attributeProbability(classCountAndAttributesCounts, attributeNumber, attributes);
                multipliedProbability *= probability;
            }

            double probability = classProbability * multipliedProbability;
            if (probability > maxProbability) {
                predictedClass = classNumber;
                maxProbability = probability;
            }
        }

        return Tuple2.of(predictedClass, new ArrayList<>(Collections.singletonList(Tuple2.of(NaiveBayesFields.AVG_CLASSIFY_DURATION, toNow(start)))));
    }


    private double attributeProbability(Tuple2<Long, ArrayList<Tuple2<Double, Double>>> classCountAndAttributesCounts, int attributeNumber, double[] attributes) {
        Tuple2<Double, Double> attributeSums = classCountAndAttributesCounts.f1.get(attributeNumber);
        double attribute = attributes[attributeNumber];
        double sum = attributeSums.f0 + attribute;
        double mean = sum / sampleNumber;
        double sumSquared = attributeSums.f1 + Math.pow(attribute, 2.0);
        double varianceNumerator = sumSquared - 2 * mean * sum + Math.pow(mean, 2.0) * sampleNumber;
        double variance = (sampleNumber == 1) ? varianceNumerator : varianceNumerator / ((double) (sampleNumber - 1L)); // Beissel correction
        return Math.exp(-Math.pow(attribute - mean, 2.0) / (2.0 * variance)) / (Math.sqrt(2 * Math.PI * variance));
    }

    @Override
    public ArrayList<Tuple2<String, Long>> train(Example example) {
        Instant start = Instant.now();

        int exampleClass = example.getMappedClass();

        Tuple2<Long, ArrayList<Tuple2<Double, Double>>> classCountAndAttributeSums = classCountAndAttributeSumsForEachClass.get(exampleClass);

        classCountAndAttributeSums.f0++;

        double[] attributes = example.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            double attributeValue = attributes[i];
            Tuple2<Double, Double> attributeValueSums = classCountAndAttributeSums.f1.get(i);
            attributeValueSums.f0 += attributeValue;
            attributeValueSums.f1 += Math.pow(attributeValue, 2.0);
            classCountAndAttributeSums.f1.set(i, attributeValueSums);
        }

        classCountAndAttributeSumsForEachClass.set(exampleClass, classCountAndAttributeSums);
        return new ArrayList<>(Collections.singletonList(Tuple2.of(NaiveBayesFields.AVG_TRAIN_DURATION, toNow(start))));
    }
}
