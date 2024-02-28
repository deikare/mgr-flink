package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.util.*;

public class SimpleNodeStatistics extends NodeStatistics {
    protected final List<List<Map<Double, Long>>> attributeValueCounts;

    public SimpleNodeStatistics(int classNumber, int attributesNumber) {
        super(classNumber);
        
        attributeValueCounts = new ArrayList<>(classNumber);
        for (int classIndex = 0; classIndex < classNumber; classIndex++) {
            List<Map<Double, Long>> classAttributesValueCounts = new ArrayList<>(attributesNumber);
            for (int attributeIndex = 0; attributeIndex < attributesNumber; attributeIndex++) {
                classAttributesValueCounts.add(new HashMap<>());
            }
            attributeValueCounts.add(classAttributesValueCounts);
        }
    }

    @Override
    protected void updateAttributeStatistics(Example example, Integer disabledAttributeIndex) {
        List<Map<Double, Long>> attributeCounts = attributeValueCounts.get(example.getMappedClass());
        if (disabledAttributeIndex == null)
            incrementAttributeCounts(example, attributeCounts, 0, attributeCounts.size(), 1L);
        else {
            incrementAttributeCounts(example, attributeCounts, 0, disabledAttributeIndex, 1L);
            incrementAttributeCounts(example, attributeCounts, disabledAttributeIndex + 1, attributeCounts.size(), 1L);
        }
    }

    private void incrementAttributeCounts(Example example, List<Map<Double, Long>> attributeCounts, int start, int end, long increment) {
        double[] exampleAttributes = example.getAttributes();
        for (int i = start; i < end; i++) {
            attributeCounts.get(i).compute(exampleAttributes[i], (key, value) -> (value == null) ? 1L : value + increment);
        }
    }

    @Override
    public double getSplittingValue(int attributeNumber) throws RuntimeException {
        return Collections.max(attributeValueCounts.get(getMajorityClass()).get(attributeNumber).entrySet(), Map.Entry.comparingByValue()).getKey(); //todo check if majority class is already calculated
    }

    @Override
    public String toString() {
        return "NodeWithAttributeValueCountsStatistics{" +
                "attributeValueCounts=" + attributeValueCounts +
                "} " + super.toString();
    }
}
