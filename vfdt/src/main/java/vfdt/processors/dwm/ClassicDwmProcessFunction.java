package vfdt.processors.dwm;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.dwm.classic.ClassicDynamicWeightedMajority;
import vfdt.classifiers.dwm.classifiers.bayes.naive.GaussianNaiveBayesClassifier;
import vfdt.processors.base.BaseProcessFunctionClassifyAndTrain;

public abstract class ClassicDwmProcessFunction extends BaseProcessFunctionClassifyAndTrain<ClassicDynamicWeightedMajority<GaussianNaiveBayesClassifier>> {
    public ClassicDwmProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<ClassicDynamicWeightedMajority<GaussianNaiveBayesClassifier>> classifierInfo = TypeInformation.of(new TypeHint<ClassicDynamicWeightedMajority<GaussianNaiveBayesClassifier>>() {
        });

        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classicDwmClassifier", classifierInfo));
    }
}
