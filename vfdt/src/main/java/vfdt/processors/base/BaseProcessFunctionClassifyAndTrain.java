package vfdt.processors.base;

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import vfdt.classifiers.base.BaseClassifierClassifyAndTrain;
import vfdt.inputs.Example;

import java.util.HashMap;

public abstract class BaseProcessFunctionClassifyAndTrain<C extends BaseClassifierClassifyAndTrain> extends BaseProcessFunction<C> {
    public BaseProcessFunctionClassifyAndTrain(String name, String dataset) {
        super(name, dataset);
    }

    @Override
    protected Tuple4<String, Integer, HashMap<String, Long>, C> processExample(Example example, C classifier) {
        Tuple3<String, Integer, HashMap<String, Long>> classifyResults = classifier.classify(example);
        HashMap<String, Long> trainingResults = classifier.train(example, classifyResults.f2);
        return new Tuple4<>(classifyResults.f0, classifyResults.f1, trainingResults, classifier);
    }
}