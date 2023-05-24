package vfdt.processors.dummies;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.classifiers.hoeffding.*;
import vfdt.inputs.Example;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class DummyHoeffdingProcessFunction extends KeyedProcessFunction<Long, Example, String> {
    private transient ValueState<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> treeState;

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeState.value();
        if (tree == null) {
            HashSet<String> attributes = new HashSet<>(Arrays.asList("period,nswprice,nswdemand,vicprice,vicdemand,transfer".split(",")));

            tree = new HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>(2, 0.95, attributes, 1, 100, new SimpleNodeStatisticsBuilder(attributes), 1) {
                @Override
                protected double heuristic(String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) {
                    return 0;
                }
            };
        }

        Tuple2<Long, HashMap<String, Long>> trainResult = tree.train(example);
        Tuple2<String, HashMap<String, Long>> classifyResult = tree.classify(example, trainResult.f1);

        collector.collect(classifyResult.f0);
    }

    @Override
    public void open(Configuration parameters) {
        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> info = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
        });

        treeState = getRuntimeContext().getState(new ValueStateDescriptor<>("tree-state", info));
    }
}
