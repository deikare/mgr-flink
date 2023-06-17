package vfdt;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.hoeffding.*;

import java.util.Arrays;
import java.util.HashSet;


public class VfdtProcessFunction extends KeyedProcessFunction<Long, Example, String> {
    private transient ValueState<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> treeValueState;

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeValueState.value();
        if (tree == null)
            tree = createTree();
        tree.train(example);
        String result = tree.predict(example);
        treeValueState.update(tree);
        String msg = "Tree predicted " + result + " on sample " + example.getClassName() + ", " + tree.getSimpleStatistics();
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
