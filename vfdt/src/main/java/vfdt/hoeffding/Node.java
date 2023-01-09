package vfdt.hoeffding;

public class Node<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> {
    Node<N_S, B> leftChild;
    Node<N_S, B> rightChild;

    double splittingValue;
    String splittingAttribute;

    private N_S statistics;
    private boolean leaf;

    public Node(B statisticsBuilder) {
        leaf = true;
        this.statistics = statisticsBuilder.build();
        leftChild = null;
        rightChild = null;
        splittingAttribute = null;
    }

    public Node<N_S, B> getChild(Example example) {
        Node<N_S, B> result = null;

        if (leaf) {
            Double attributeValue = example.getAttributes().get(splittingAttribute);
            if (attributeValue != null) {
                result = (attributeValue <= splittingValue)? leftChild : rightChild;
            }
        }

        return result;
    }

    public N_S getStatistics() {
        return statistics;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public void splitLeaf(String splittingAttribute, B statisticsBuilder) {
        this.leaf = false;
        this.splittingAttribute = splittingAttribute;
        this.splittingValue = statistics.getSplittingValue(splittingAttribute);
        this.leftChild = new Node<>(statisticsBuilder);
        this.rightChild = new Node<>(statisticsBuilder);
    }


    public String getMajorityClass() {
        return statistics.getMajorityClass();
    }
}
