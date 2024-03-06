package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.bstHoeffding.BstHoeffdingTree;
import vfdt.classifiers.bstHoeffding.NaiveBayesNodeStatistics;
import vfdt.classifiers.bstHoeffding.NaiveBayesNodeStatisticsBuilder;
import vfdt.processors.base.BaseProcessFunctionTrainAndClassify;

public abstract class VfdtBstProcessFunction extends BaseProcessFunctionTrainAndClassify<BstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>> {
    public VfdtBstProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<BstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<BstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("vfdtBstClassifier", classifierInfo));
    }
}
