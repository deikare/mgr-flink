package vfdt.classifiers.bstHoeffding.statistics;

import vfdt.inputs.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class BstHoeffdingTreeStatistics {
    private final List<AttributeValuesCountBst> attributeCountsTrees;

    public BstHoeffdingTreeStatistics(int attributeNumber) {
        attributeCountsTrees = new ArrayList<>(attributeNumber);
        for (int attributeIndex = 0; attributeIndex < attributeNumber; attributeIndex++) {
            attributeCountsTrees.add(new AttributeValuesCountBst());
        }
    }

    public void updateStatistics(Example example, int classNumber) {
        int exampleClass = example.getMappedClass();
        double[] attributes = example.getAttributes();
        for (int attributeIndex = 0; attributeIndex < attributes.length; attributeIndex++) {
            attributeCountsTrees.get(attributeIndex).insertValue(attributes[attributeIndex], exampleClass, classNumber);
        }
    }

    public double getSplittingValue(int attributeIndex, int classNumber, long n) {
        double bestAttributeValue = Double.MIN_VALUE;

        double maxInfo = -Double.MAX_VALUE;

        Stack<AttributeCountsNode> stack = new Stack<>();
        stack.add(attributeCountsTrees.get(attributeIndex).getRoot());

        while (!stack.isEmpty()) {
            AttributeCountsNode node = stack.pop();

            double currentInfo = calculateInformation(node.ve, classNumber, n) + calculateInformation(node.vh, classNumber, n);

            if (currentInfo > maxInfo) {
                bestAttributeValue = node.value;
                maxInfo = currentInfo;
            }

            pushIfNotNull(node.leftChild, stack);
            pushIfNotNull(node.rightChild, stack);
        }

        return bestAttributeValue;
    }

    private double calculateInformation(long[] attributeCounts, int classNumber, long n) {
        long totalCount = 0;
        for (long count : attributeCounts) {
            totalCount += count;
        }
        double base = Math.log(2);
        double totalCountAsDouble = (double) totalCount;

        double result = 0.0;
        for (int classIndex = 0; classIndex < classNumber; classIndex++) {
            long ve = attributeCounts[classIndex];
            double probability = ((double) ve) / (totalCountAsDouble);
            result += probability * Math.log(probability) / base;
        }

        return result * totalCountAsDouble / ((double) n);
    }

    private void pushIfNotNull(AttributeCountsNode item, Stack<AttributeCountsNode> stack) {
        if (item != null)
            stack.push(item);
    }
}
