package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.util.Arrays;

public class NodeStatistics implements StatisticsInterface {
    private long n;

    private final long[] classCounts;

    public NodeStatistics(int classNumber) {
        n = 0;
        classCounts = new long[classNumber];
    }

    public void update(Example example) {
        n++;
        classCounts[example.getMappedClass()] += 1L;
    }

    @Override
    public int getMajorityClass() {
        int majorityClass = 0;
        long majorityCount = classCounts[0];

        for (int i = 1; i < classCounts.length; i++) {
            if (classCounts[i] > majorityCount) {
                majorityClass = i;
                majorityCount = classCounts[i];
            }
        }
        return majorityClass;
    }

    @Override
    public double getSplittingValue(int attributeNumber) {
        return 0;
    }

    public long getN() {
        return n;
    }

    public void resetN() {
        n = 0L;
    }

    @Override
    public String toString() {
        return "NodeStatistics{" +
                "n=" + n +
                ", classCounts=" + Arrays.toString(classCounts) +
                '}';
    }
}
