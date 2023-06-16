package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.hoeffding.HoeffdingTree;
import vfdt.classifiers.hoeffding.SimpleNodeStatistics;
import vfdt.classifiers.hoeffding.SimpleNodeStatisticsBuilder;
import vfdt.processors.base.BaseProcessFunction;

public abstract class VfdtProcessFunction extends BaseProcessFunction<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> {
    public VfdtProcessFunction(String name, String dataset) {
        super(name, dataset);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classifier", classifierInfo));
    }
}
