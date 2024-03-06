package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.bstHoeffding.standard.BstHoeffdingTree;
import vfdt.classifiers.hoeffding.SimpleNodeStatistics;
import vfdt.classifiers.hoeffding.SimpleNodeStatisticsBuilder;
import vfdt.processors.base.BaseProcessFunctionTrainAndClassify;

public abstract class VfdtBstProcessFunction extends BaseProcessFunctionTrainAndClassify<BstHoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> {
    public VfdtBstProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<BstHoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<BstHoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("vfdtBstClassifier", classifierInfo));
    }
}
