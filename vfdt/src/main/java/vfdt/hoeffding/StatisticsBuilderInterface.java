package vfdt.hoeffding;

import java.io.Serializable;

public interface StatisticsBuilderInterface<S extends NodeStatistics> extends Serializable {
    S build();
}
