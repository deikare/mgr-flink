package vfdt.processors.base;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.classifiers.base.BaseClassifier;
import vfdt.classifiers.base.BaseClassifierTags;
import vfdt.inputs.Example;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

public abstract class BaseProcessFunction<C extends BaseClassifier> extends KeyedProcessFunction<Long, Example, String> {
    protected transient ValueState<C> classifierState;
    protected String name;
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

        Tuple4<String, String, HashMap<String, Long>, C> results = processExample(example, classifier);
        classifierState.update(results.f3);

        String msg = produceMessage(classifier, results.f0, results.f1, example.getClassName(), results.f2);
        collector.collect(msg);
    }

    protected abstract Tuple4<String, String, HashMap<String, Long>, C> processExample(Example example, C classifier);

    protected abstract C createClassifier();

    protected String produceMessage(C classifier, String timestamp, String predicted, String exampleClass, HashMap<String, Long> performances) throws IOException {
        String result = "classifierResult";
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_NAME, name);
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_PARAMS, classifier.generateClassifierParams());
        result += "," + produceTag(BaseClassifierTags.JOB_ID, getRuntimeContext().getJobId().toString());
        result += "," + produceTag(BaseClassifierTags.DATASET, dataset);
        result += "," + produceTag(BaseClassifierTags.CLASS, exampleClass);
        result += "," + produceTag(BaseClassifierTags.PREDICTED, predicted) + " ";

        result += performances.entrySet().stream().map(entry -> produceFieldAsNumber(entry.getKey(), entry.getValue(), "i")).collect(Collectors.joining(",")) + " ";

        result += timestamp;

        return result;
    }

    protected <T> String produceTag(String key, T value) {
        return key + "=" + value;
    }

    protected <T> String produceFieldAsString(String key, T value) {
        return key + "=\"" + value + "\"";
    }

    protected <T> String produceFieldAsNumber(String key, T value, String unit) {
        return key + "=" + value + unit;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        registerClassifier();
    }

    protected abstract void registerClassifier(); //its abstract because TypeInfo cannot be templated
}
