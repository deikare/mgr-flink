package vfdt.hoeffding;

public class Node<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> {
    private Node<N_S, B> leftChild;
    private Node<N_S, B> rightChild;

    private double splittingValue;
    private String splittingAttribute;

    private final N_S statistics;

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
