package vfdt;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.hoeffding.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BiFunction;


public class VFDT extends KeyedProcessFunction<Long, Example, String> {
    private transient ValueState<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> treeValueState;

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeValueState.value();
//        if (tree == null)
//            tree = new HoeffdingTree<>();
//        tree.train(example);
//        tree.predict(example);
        treeValueState.update(tree);
    }

    @Override
    public void open(Configuration parameters) {
        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> info = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
            @Override
            public TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> getTypeInfo() {
                return super.getTypeInfo();
            }

            @Override
            public int hashCode() {
                return super.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            public String toString() {
                return super.toString();
            }
        });

        ParameterTool params = (ParameterTool) getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        long classesNumber = Long.parseLong(params.get("classesNumber", "2"));
        double delta = Double.parseDouble(params.get("delta", "0.05"));
        double tau = Double.parseDouble(params.get("tau", "0.2"));
        long nMin = Long.parseLong(params.get("nMin", "50"));
        long batchStatLength = Long.parseLong(params.get("batchStatLength", "500"));
        HashSet<String> attributes = new HashSet<>(Arrays.asList(params.get("attributes").split(",")));

        SerializableHeuristic<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> heuristic = (s, node) -> {
            double threshold = 0.5;
            return Math.abs(threshold - node.getStatistics().getSplittingValue(s)) / threshold;
        };

        SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);

        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>(classesNumber, delta, attributes, tau, nMin, statisticsBuilder, heuristic, batchStatLength);


        treeValueState = getRuntimeContext().getState(new ValueStateDescriptor<>("tree-state", info, tree)); //beware of deprecated
//        treeValueState = getRuntimeContext().getState(new ValueStateDescriptor<>("tree-state", info));
    }
}
