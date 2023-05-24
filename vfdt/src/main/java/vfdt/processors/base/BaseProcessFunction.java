package vfdt.processors.base;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.classifiers.base.BaseClassifier;
import vfdt.classifiers.base.BaseClassifierTags;
import vfdt.inputs.Example;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class BaseProcessFunction<C extends BaseClassifier> extends KeyedProcessFunction<Long, Example, String> {
    protected transient ValueState<C> classifierState;
    protected String name;
    protected String experimentId = UUID.randomUUID().toString();
    protected String dataset;

    public BaseProcessFunction(String name, String dataset) {
        this.name = name;
        this.dataset = dataset;
    }

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        C classifier = classifierState.value();
        if (classifier == null)
            classifier = createClassifier();

        Tuple2<Long, HashMap<String, Long>> trainingResult = classifier.train(example);
        Tuple2<String, HashMap<String, Long>> classifyResult = classifier.classify(example, trainingResult.f1);
        classifierState.update(classifier);

        String msg = produceMessage(trainingResult.f0, classifyResult, example.getClassName());

        collector.collect(msg);
    }

    protected abstract C createClassifier();

    private String produceMessage(Long timestamp, Tuple2<String, HashMap<String, Long>> classifyResult, String exampleClass) throws IOException {
        String result = name;
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_PARAMS, classifierState.value().generateClassifierParams());
        result += "," + produceTag(BaseClassifierTags.EXPERIMENT_ID, experimentId);
        result += "," + produceTag(BaseClassifierTags.DATASET, dataset);
        result += "," + produceTag(BaseClassifierTags.CLASS, exampleClass);
        result += "," + produceTag(BaseClassifierTags.PREDICTED, classifyResult.f0) + " ";

        result += classifyResult.f1.entrySet().stream().map(entry -> produceTag(entry.getKey(), entry.getValue())).collect(Collectors.joining(",")) + " ";
        result += timestamp;

        return result;
    }

    private <T> String produceTag(String key, T value) {
        return key + "=" + value;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        registerClassifier();
    }

    protected abstract void registerClassifier(); //its abstract because TypeInfo cannot be templated
}
