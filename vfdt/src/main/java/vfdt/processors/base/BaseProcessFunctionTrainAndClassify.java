package vfdt.processors.base;

import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import vfdt.classifiers.base.BaseClassifierTrainAndClassify;
import vfdt.inputs.Example;

import java.util.ArrayList;

public abstract class BaseProcessFunctionTrainAndClassify<C extends BaseClassifierTrainAndClassify> extends BaseProcessFunction<C> {
    public BaseProcessFunctionTrainAndClassify(String name, String dataset) {
        super(name, dataset);
    }

    @Override
    protected Tuple4<String, Integer, ArrayList<Tuple2<String, Long>>, C> processExample(Example example, C classifier) {
        Tuple2<String, ArrayList<Tuple2<String, Long>>> trainingResult = classifier.train(example);
        Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyResult = classifier.classify(example, trainingResult.f1);
        return new Tuple4<>(trainingResult.f0, classifyResult.f0, classifyResult.f1, classifier);
    }
}
