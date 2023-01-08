package vfdt.hoeffding;

import java.util.HashSet;

public interface StatisticsBuilderInterface<S extends NodeStatistics> {
    S build(HashSet<String> classNames);
}
