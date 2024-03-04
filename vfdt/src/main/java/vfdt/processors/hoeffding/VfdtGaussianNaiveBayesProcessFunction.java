package vfdt.processors.hoeffding;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.hoeffding.*;
import vfdt.processors.base.BaseProcessFunctionTrainAndClassify;

public abstract class VfdtGaussianNaiveBayesProcessFunction extends BaseProcessFunctionTrainAndClassify<HoeffdingTree<GaussianNaiveBayesStatistics, GaussianNaiveBayesStatisticsBuilder>> {

    public VfdtGaussianNaiveBayesProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<HoeffdingTree<GaussianNaiveBayesStatistics, GaussianNaiveBayesStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<HoeffdingTree<GaussianNaiveBayesStatistics, GaussianNaiveBayesStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("vfdtGaussNbClassifier", classifierInfo));
    }
}
