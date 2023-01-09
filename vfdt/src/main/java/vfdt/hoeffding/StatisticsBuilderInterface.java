package vfdt.hoeffding;

public interface StatisticsBuilderInterface<S extends NodeStatistics> {
    S build();
}
