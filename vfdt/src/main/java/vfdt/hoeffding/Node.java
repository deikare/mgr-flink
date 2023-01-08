package vfdt.hoeffding;

import java.util.ArrayList;

public class Node<S extends NodeStatistics, C extends ComparatorInterface> {
    private ArrayList<Node<S, C>> children;

    private S statistics;
    private C comparator;
    private boolean leaf;

    public Node(S statistics) {
        leaf = true;
        this.statistics = statistics;
    }

    public Node<S, C> getChild(Example example) {
        return leaf? null : children.get(comparator.getChild((example)));
    }

    public ArrayList<Node< S, C>> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<Node< S, C>> children) {
        this.children = children;
    }

    public S getStatistics() {
        return statistics;
    }

    public void setStatistics(S statistics) {
        this.statistics = statistics;
    }

    public C getComparator() {
        return comparator;
    }

    public void setComparator(C comparator) {
        this.comparator = comparator;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    public void splitLeaf() {}


    public String getMajorityClass() {
        return statistics.getMajorityClass();
    }
}
