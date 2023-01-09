package vfdt.hoeffding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class NodeWithAttributeValueCountsStatistics extends NodeStatistics {
    private HashMap<String, HashMap<Double, Long>> attributeValueCounts;

    public NodeWithAttributeValueCountsStatistics(HashSet<String> classNames, HashSet<String> attributes) {
        super(classNames);
        attributeValueCounts = new HashMap<>();
        for (String attribute : attributes) {
            HashMap<Double, Long> attributeValuesCounter = new HashMap<>();
            attributeValueCounts.put(attribute, attributeValuesCounter);
        }
    }

    @Override
    public void update(Example example) throws RuntimeException {
        super.update(example);
        for (Map.Entry<String, Double> attributeEntry : example.getAttributes().entrySet()) {
            HashMap<Double, Long> attributeValuesCounter = getAttributeValuesCounter(attributeEntry.getKey());

            Double value = attributeEntry.getValue();
            Long valueCounts = attributeValuesCounter.get(value);
            if (valueCounts == null) {
                attributeValuesCounter.put(value, 0L);
            } else {
                attributeValuesCounter.put(value, valueCounts + 1);
            }
            //TODO verify, if attributeValueCounts needs to be updated on internal map update
        }
    }

    private HashMap<Double, Long> getAttributeValuesCounter(String attribute) throws RuntimeException {
        HashMap<Double, Long> attributeValuesCounter = attributeValueCounts.get(attribute);
        if (attributeValuesCounter == null) {
            throw new RuntimeException("Example contains undefined attribute");
        }
        return attributeValuesCounter;
    }

    @Override
    public double getSplittingValue(String attribute) throws RuntimeException {
        HashMap<Double, Long> attributeValuesCounter = getAttributeValuesCounter(attribute);
        return Collections.max(attributeValuesCounter.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
}
