package vfdt.hoeffding;

import java.util.ArrayList;

public class Node<K, S extends NodeStatistics<K>, C extends ComparatorInterface<K>> {
    private ArrayList<Node<K, S, C>> children;

    private S statistics;
    private C comparator;
    private boolean leaf;

    public Node(S statistics) {
        leaf = true;
        this.statistics = statistics;
    }

    public Node<K, S, C> getChild(Example<K> example) {
        return leaf? null : children.get(comparator.getChild((example)));
    }

    public ArrayList<Node<K, S, C>> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<Node<K, S, C>> children) {
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
