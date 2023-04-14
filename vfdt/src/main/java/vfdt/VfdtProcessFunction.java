package vfdt;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.tuple.Tuple8;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.hoeffding.*;

import java.util.Arrays;
import java.util.HashSet;


public class VfdtProcessFunction extends KeyedProcessFunction<Long, Example, HoeffdingTreeProcessOutputJson> {
    private transient ValueState<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> treeValueState;

    //todo write base processfunction, that has e.g. experimentId generator
    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, HoeffdingTreeProcessOutputJson>.Context context, Collector<HoeffdingTreeProcessOutputJson> collector) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeValueState.value();
        if (tree == null)
            tree = createTree();
        Tuple4<Long, Long, Long, Long> trainResult = tree.train(example);
        Tuple4<Long, Long, Long, Long> predictResult = tree.predict(example);
        treeValueState.update(tree);
        HoeffdingTreeProcessOutputJson msg = new Tuple8<>(trainResult.f0, trainResult.f1, trainResult.f2, trainResult.f3, predictResult.f0, predictResult.f1, predictResult.f2, predictResult.f3);
        collector.collect(msg); //TODO print collector
    }

    @Override
    public void close() throws Exception {
        super.close();
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeValueState.value();
        if (tree != null)
            tree.printStatisticsToFile("/home/deikare/wut/streaming-datasets/" + "elec.csv");
    }

    private HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> createTree() {
        ParameterTool params = (ParameterTool) getRuntimeContext().getExecutionConfig().getGlobalJobParameters();
        long classesNumber = params.getLong("classesNumber", 2);
        double delta = params.getDouble("delta", 0.05);
        double tau = params.getDouble("tau", 0.2);
        long nMin = params.getLong("nMin", 50);
        long batchStatLength = params.getLong("batchStatLength", 500);
        HashSet<String> attributes = new HashSet<>(Arrays.asList(params.get("attributes").split(",")));

        SerializableHeuristic<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> heuristic = (s, node) -> {
            double threshold = 0.5;
            return Math.abs(threshold - node.getStatistics().getSplittingValue(s)) / threshold;
        };

        SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);

        return new HoeffdingTree<>(classesNumber, delta, attributes, tau, nMin, statisticsBuilder, heuristic, batchStatLength);
    }

    @Override
    public void open(Configuration parameters) {

        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> info = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
        });

        treeValueState = getRuntimeContext().getState(new ValueStateDescriptor<>("tree-state", info));
    }
}
