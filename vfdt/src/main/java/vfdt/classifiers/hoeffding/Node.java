package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.io.Serializable;

public class Node<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> implements Serializable {
    private Node<N_S, B> leftChild;
    private Node<N_S, B> rightChild;

    private double splittingValue;
    private String splittingAttribute;

    private N_S statistics;

    public Node() {
    }

    public Node<N_S, B> getLeftChild() {
        return leftChild;
    }

    public void setLeftChild(Node<N_S, B> leftChild) {
        this.leftChild = leftChild;
    }

    public Node<N_S, B> getRightChild() {
        return rightChild;
    }

    public void setRightChild(Node<N_S, B> rightChild) {
        this.rightChild = rightChild;
    }

    public double getSplittingValue() {
        return splittingValue;
    }

    public void setSplittingValue(double splittingValue) {
        this.splittingValue = splittingValue;
    }

    public String getSplittingAttribute() {
        return splittingAttribute;
    }

    public void setSplittingAttribute(String splittingAttribute) {
        this.splittingAttribute = splittingAttribute;
    }

    public void setStatistics(N_S statistics) {
        this.statistics = statistics;
    }

    public Node(B statisticsBuilder) {
        this.statistics = statisticsBuilder.build();
        leftChild = null;
        rightChild = null;
        splittingAttribute = null;
    }

    public Node<N_S, B> getChild(Example example) {
        Node<N_S, B> result = null;

        if (!isLeaf()) {
            Double attributeValue = example.getAttributes().get(splittingAttribute);
            if (attributeValue != null) {
                result = (attributeValue <= splittingValue) ? leftChild : rightChild;
            }
        }

        return result;
    }

    public N_S getStatistics() {
        return statistics;
    }

    public boolean isLeaf() {
        return leftChild == null || rightChild == null;
    }

    public void split(String splittingAttribute, B statisticsBuilder, Example example) {
        split(splittingAttribute, statisticsBuilder);
        Node<N_S, B> child = getChild(example);
        child.updateStatistics(example);
    }

    private void split(String splittingAttribute, B statisticsBuilder) {
        this.splittingAttribute = splittingAttribute;
        this.splittingValue = statistics.getSplittingValue(splittingAttribute);
        this.leftChild = new Node<>(statisticsBuilder);
        this.rightChild = new Node<>(statisticsBuilder);
    }

    public void updateStatistics(Example example) {
        statistics.update(example);
    }


    public String getMajorityClass() {
        return statistics.getMajorityClass();
    }

    public long getN() {
        return statistics.getN();
    }

    public void resetN() {
        statistics.resetN();
    }
}
