package vfdt.hoeffding;

import java.io.Serializable;
import java.util.function.BiFunction;

public interface SerializableHeuristic<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends BiFunction<String, Node<N_S, B>, Double>, Serializable {
}
