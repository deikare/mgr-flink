package vfdt.processors.base;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import vfdt.classifiers.base.BaseClassifierTrainAndClassify;
import vfdt.inputs.Example;

import java.util.HashMap;

public abstract class BaseProcessFunctionTrainAndClassify<C extends BaseClassifierTrainAndClassify> extends BaseProcessFunction<C> {
    public BaseProcessFunctionTrainAndClassify(String name, String dataset) {
        super(name, dataset);
    }

    @Override
    protected Tuple4<String, Integer, HashMap<String, Long>, C> processExample(Example example, C classifier) {
        Tuple2<String, HashMap<String, Long>> trainingResult = classifier.train(example);
        Tuple2<Integer, HashMap<String, Long>> classifyResult = classifier.classify(example, trainingResult.f1);
        return new Tuple4<>(trainingResult.f0, classifyResult.f0, classifyResult.f1, classifier);
    }
}