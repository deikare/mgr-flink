package vfdt.hoeffding;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class NodeStatistics implements StatisticsInterface {
    private long n;

    private HashMap<String, Long> classCounts;
    public NodeStatistics(HashSet<String> classNames) {
        n = 0;
        classCounts = new HashMap<>();
        for (String className : classNames)
            classCounts.put(className, 0L);
    }

    public void update(Example example) {
        n += 1;
        String exampleClass = example.getClassName();
        classCounts.put(exampleClass, classCounts.get(exampleClass) + 1L);
    }

    @Override
    public String getMajorityClass() {
        return Collections.max(classCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    @Override
    public double getSplittingValue(String attribute) {
        return 0;
    }

    public long getN() {
        return n;
    }
}
