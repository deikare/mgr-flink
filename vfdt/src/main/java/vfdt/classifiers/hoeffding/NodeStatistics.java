package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.util.Arrays;

import static vfdt.classifiers.helpers.Helpers.getIndexOfHighestValue;

public class NodeStatistics implements StatisticsInterface {
    private long n;

    private final Long[] classCounts;

    public NodeStatistics(int classNumber) {
        n = 0;
        classCounts = new Long[classNumber];
        for (int i = 0; i < classNumber; i++)
            classCounts[i] = 0L;
    }

    public void update(Example example) {
        n++;
        classCounts[example.getMappedClass()] += 1L;
    }

    @Override
    public int getMajorityClass() {
        return getIndexOfHighestValue(classCounts);
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
