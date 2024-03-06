package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.bstHoeffding.functional.FunctionalBstHoeffdingTree;
import vfdt.classifiers.bstHoeffding.functional.NaiveBayesNodeStatistics;
import vfdt.classifiers.bstHoeffding.functional.NaiveBayesNodeStatisticsBuilder;
import vfdt.processors.base.BaseProcessFunctionTrainAndClassify;

public abstract class FunctionalVfdtBstProcessFunction extends BaseProcessFunctionTrainAndClassify<FunctionalBstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>> {
    public FunctionalVfdtBstProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<FunctionalBstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<FunctionalBstHoeffdingTree<NaiveBayesNodeStatistics, NaiveBayesNodeStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("functionalVfdtBstClassifier", classifierInfo));
    }
}
