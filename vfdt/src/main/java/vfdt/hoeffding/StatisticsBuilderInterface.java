package vfdt.hoeffding;

import java.util.HashSet;

public interface StatisticsBuilderInterface<K, S extends NodeStatistics<K>> {
    S build(HashSet<String> classNames);
}
