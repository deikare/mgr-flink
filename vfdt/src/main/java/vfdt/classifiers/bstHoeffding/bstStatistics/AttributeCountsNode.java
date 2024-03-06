package vfdt.classifiers.bstHoeffding.bstStatistics;

public class AttributeCountsNode {
    public final double value;

    public long[] ve;
    public long[] vh;

    public AttributeCountsNode leftChild;
    public AttributeCountsNode rightChild;

    public AttributeCountsNode(double value, int classIndex, int classNumber) {
        this.value = value;

        this.ve = new long[classNumber];
        this.ve[classIndex]++;
        this.vh = new long[classNumber];

        this.leftChild = null;
        this.rightChild = null;
    }
}
