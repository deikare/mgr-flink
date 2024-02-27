package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.util.*;

//todo think about encoding all attribute and class labels from string to index in arrays so
// all hashmaps can be transformed to arrays
public class SimpleNodeStatistics extends NodeStatistics {
    private final List<Map<Double, Long>> attributeValueCounts;

    public SimpleNodeStatistics(int classNumber) {
        super(classNumber);
        attributeValueCounts = new ArrayList<>(classNumber);
        for (int i = 0; i < classNumber; i++)
            attributeValueCounts.add(i, new HashMap<>());
    }

    @Override
    protected void updateAttributeStatistics(Example example, Integer disabledAttributeIndex) {
        if (disabledAttributeIndex == null)
            incrementAttributeCounts(example, 0, attributeValueCounts.size());
        else {
            incrementAttributeCounts(example, 0, disabledAttributeIndex);
            incrementAttributeCounts(example, disabledAttributeIndex + 1, attributeValueCounts.size());
        }
    }

    private void incrementAttributeCounts(Example example, int start, int end) {
        double[] exampleAttributes = example.getAttributes();
        for (int i = start; i < end; i++) {
            attributeValueCounts.get(i).compute(exampleAttributes[i], (key, value) -> (value == null) ? 1L : value + 1L);
        }
    }

    @Override
    public double getSplittingValue(int attributeNumber) throws RuntimeException {
        return Collections.max(attributeValueCounts.get(attributeNumber).entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    @Override
    public String toString() {
        return "NodeWithAttributeValueCountsStatistics{" +
                "attributeValueCounts=" + attributeValueCounts +
                "} " + super.toString();
    }
}
