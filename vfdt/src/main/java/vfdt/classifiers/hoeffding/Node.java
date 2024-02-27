package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.io.Serializable;

public class Node<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> implements Serializable {
    private Node<N_S, B> leftChild;
    private Node<N_S, B> rightChild;

    private double splittingValue;
    private Integer splittingAttributeNumber;

    private final Integer disabledAttributeIndex;

    private final N_S statistics;

    public Node(B statisticsBuilder, Integer disabledAttributeIndex) {
        this.statistics = statisticsBuilder.build();
        leftChild = null;
        rightChild = null;
        splittingAttributeNumber = null;
        this.disabledAttributeIndex = disabledAttributeIndex;
    }

    public Integer getDisabledAttributeIndex() {
        return disabledAttributeIndex;
    }

    public Node<N_S, B> getChild(Example example) {
        Node<N_S, B> result = null;

        if (!isLeaf()) {
            double attributeValue = example.getAttributes()[splittingAttributeNumber];
            result = (attributeValue <= splittingValue) ? leftChild : rightChild;
        }

        return result;
    }

    public N_S getStatistics() {
        return statistics;
    }

    public boolean isLeaf() {
        return leftChild == null || rightChild == null;
    }

    public void split(int splittingAttributeNumber, B statisticsBuilder, Example example) {
        split(splittingAttributeNumber, statisticsBuilder);
        Node<N_S, B> child = getChild(example);
        child.updateStatistics(example);
    }

    private void split(int splittingAttributeNumber, B statisticsBuilder) {
        this.splittingAttributeNumber = splittingAttributeNumber;
        this.splittingValue = statistics.getSplittingValue(splittingAttributeNumber);
        this.leftChild = new Node<>(statisticsBuilder, splittingAttributeNumber);
        this.rightChild = new Node<>(statisticsBuilder, splittingAttributeNumber);
    }

    public void updateStatistics(Example example) {
        statistics.update(example, disabledAttributeIndex);
    }


    public int getMajorityClass() {
        return statistics.getMajorityClass();
    }

    public long getN() {
        return statistics.getN();
    }

    public void resetN() {
        statistics.resetN();
    }
}
