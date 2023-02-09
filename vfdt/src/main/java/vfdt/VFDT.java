package vfdt;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.hoeffding.Example;
import vfdt.hoeffding.HoeffdingTree;
import vfdt.hoeffding.SimpleNodeStatistics;
import vfdt.hoeffding.SimpleNodeStatisticsBuilder;

public class VFDT extends ProcessFunction<Example, String> implements CheckpointedFunction {
    private transient ValueState<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> treeValueState;

    @Override
    public void processElement(Example example, ProcessFunction<Example, String>.Context context, Collector<String> collector) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = treeValueState.value();
        tree.train(example);
        tree.predict(example);
        treeValueState.update(tree);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>();
        treeValueState.update(tree);
    }

    @Override
    public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {

    }

    @Override
    public void initializeState(FunctionInitializationContext functionInitializationContext) throws Exception {

    }
}
