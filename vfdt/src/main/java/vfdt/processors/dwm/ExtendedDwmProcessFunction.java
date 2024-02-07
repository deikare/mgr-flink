package vfdt.processors.dwm;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.dwm.classifiers.bayes.naive.GaussianNaiveBayesClassifier;
import vfdt.classifiers.dwm.extended.ExtendedDynamicWeightedMajority;
import vfdt.processors.base.BaseProcessFunctionClassifyAndTrain;

public abstract class ExtendedDwmProcessFunction extends BaseProcessFunctionClassifyAndTrain<ExtendedDynamicWeightedMajority<GaussianNaiveBayesClassifier>> {
    public ExtendedDwmProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<ExtendedDynamicWeightedMajority<GaussianNaiveBayesClassifier>> classifierInfo = TypeInformation.of(new TypeHint<ExtendedDynamicWeightedMajority<GaussianNaiveBayesClassifier>>() {
        });

        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("extendedDwmClassifier", classifierInfo));
    }
}
