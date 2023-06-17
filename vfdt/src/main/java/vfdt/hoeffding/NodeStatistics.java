package vfdt.hoeffding;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NodeStatistics implements StatisticsInterface {
    private long n;

    private final HashMap<String, Long> classCounts;

    public NodeStatistics() {
        n = 0;
        classCounts = new HashMap<>();
    }

    public void update(Example example) {
        n++;
        String exampleClass = example.getClassName();
        classCounts.merge(exampleClass, 1L, Long::sum);

    }

    @Override
    public String getMajorityClass() {
        String result = null;
        if (!classCounts.isEmpty())
            result = Collections.max(classCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
        return result;
    }

    @Override
    public double getSplittingValue(String attribute) {
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
                ", classCounts=" + classCounts +
                '}';
    }
}
