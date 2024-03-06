package vfdt.classifiers.bstHoeffding.statistics;

public class AttributeValuesCountBst {
    private AttributeCountsNode root;

    public AttributeValuesCountBst() {
        root = null;
    }

    public void insertValue(double value, int classIndex, int classNumber) {
        if (root == null)
            root = new AttributeCountsNode(value, classIndex, classNumber);
        else {
            AttributeCountsNode tmp = root;
            while (true) {
                if (value > tmp.value) {
                    tmp.vh[classIndex]++;
                    if (tmp.rightChild == null) {
                        tmp.rightChild = new AttributeCountsNode(value, classIndex, classNumber);
                        break;
                    } else tmp = tmp.rightChild;
                } else {
                    tmp.ve[classIndex]++;
                    if (value == tmp.value)
                        break;
                    else {
                        if (tmp.leftChild == null) {
                            tmp.leftChild = new AttributeCountsNode(value, classIndex, classNumber);
                            break;
                        } else tmp = tmp.leftChild;
                    }
                }
            }
        }
    }

    public AttributeCountsNode getRoot() {
        return root;
    }
}
