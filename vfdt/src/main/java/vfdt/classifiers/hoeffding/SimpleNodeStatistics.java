package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.util.*;

public class SimpleNodeStatistics extends NodeStatistics {
    protected final List<List<Map<Double, Long>>> attributeValueCounts;

    public SimpleNodeStatistics(int classNumber, int attributesNumber) {
        super(classNumber);

        attributeValueCounts = new ArrayList<>(classNumber);
        for (int classIndex = 0; classIndex < classNumber; classIndex++) {
            List<Map<Double, Long>> attributeValuesForEachAttribute = new ArrayList<>(attributesNumber);
            for (int attributeIndex = 0; attributeIndex < attributesNumber; attributeIndex++) {
                attributeValuesForEachAttribute.add(new HashMap<>());
            }
            attributeValueCounts.add(attributeValuesForEachAttribute);
        }
    }

    @Override
    protected void updateAttributeStatistics(Example example, Integer disabledAttributeIndex) {
        List<Map<Double, Long>> attributeValues = attributeValueCounts.get(example.getMappedClass());
        if (disabledAttributeIndex == null)
            incrementAttributeCounts(example, attributeValues, 0, attributeValues.size(), 1L);
        else {
            incrementAttributeCounts(example, attributeValues, 0, disabledAttributeIndex, 1L);
            incrementAttributeCounts(example, attributeValues, disabledAttributeIndex + 1, attributeValues.size(), 1L);
        }
    }

    private void incrementAttributeCounts(Example example, List<Map<Double, Long>> attributeValues, int start, int end, long increment) {
        double[] exampleAttributes = example.getAttributes();
        for (int attributeIndex = start; attributeIndex < end; attributeIndex++) {
            attributeValues.get(attributeIndex).compute(exampleAttributes[attributeIndex], (key, value) -> (value == null) ? 1L : value + increment);
        }
    }

    @Override
    public double getSplittingValue(int attributeNumber) throws RuntimeException {
        return Collections.max(attributeValueCounts.get(getMajorityClass()).get(attributeNumber).entrySet(), Map.Entry.comparingByValue()).getKey(); //todo check if majority class is already calculated
    }

    public List<List<Map<Double, Long>>> getAttributeValueCounts() {
        return attributeValueCounts;
    }

    @Override
    public String toString() {
        return "NodeWithAttributeValueCountsStatistics{" +
                "attributeValueCounts=" + attributeValueCounts +
                "} " + super.toString();
    }
}
