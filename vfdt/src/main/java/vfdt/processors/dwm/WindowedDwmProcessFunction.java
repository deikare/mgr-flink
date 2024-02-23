package vfdt.processors.dwm;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import vfdt.classifiers.dwm.classifiers.bayes.naive.GaussianNaiveBayesClassifier;
import vfdt.classifiers.dwm.windowed.WindowedDynamicWeightedMajority;
import vfdt.processors.base.BaseProcessFunctionClassifyAndTrain;

public abstract class WindowedDwmProcessFunction extends BaseProcessFunctionClassifyAndTrain<WindowedDynamicWeightedMajority<GaussianNaiveBayesClassifier>> {
    public WindowedDwmProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        super(name, dataset, bootstrapSamplesLimit);
    }

    @Override
    protected void registerClassifier() {
        TypeInformation<WindowedDynamicWeightedMajority<GaussianNaiveBayesClassifier>> classifierInfo = TypeInformation.of(new TypeHint<WindowedDynamicWeightedMajority<GaussianNaiveBayesClassifier>>() {
        });

        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("windowedDwmClassifier", classifierInfo));
    }
}
