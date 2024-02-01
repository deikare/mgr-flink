package vfdt.processors.base;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.classifiers.base.BaseClassifier;
import vfdt.classifiers.base.BaseClassifierTags;
import vfdt.inputs.Example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public abstract class BaseProcessFunction<C extends BaseClassifier> extends KeyedProcessFunction<Long, Example, String> {
    protected transient ValueState<C> classifierState;
    protected transient ValueState<Long> sampleNumberState;

    protected String name;
    protected String dataset;
    protected long bootstrapSamplesLimit;

    public BaseProcessFunction(String name, String dataset, long bootstrapSamplesLimit) {
        this.name = name;
        this.dataset = dataset;
        this.bootstrapSamplesLimit = bootstrapSamplesLimit;
    }


    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        Long sampleNumber = sampleNumberState.value();
        if (sampleNumber == null)
            sampleNumber = 1L;
        else sampleNumber++;

        sampleNumberState.update(sampleNumber);

        C classifier = classifierState.value();
        if (classifier == null)
            classifier = createClassifier();

        if (sampleNumber <= bootstrapSamplesLimit)
            classifier.bootstrapTrainImplementation(example);
        else {
            Tuple4<String, Integer, ArrayList<Tuple2<String, Long>>, C> results = processExample(example, classifier);
            classifierState.update(results.f3);

            String msg = produceMessage(results.f3, results.f0, results.f1, example.getMappedClass(), results.f2);
            collector.collect(msg);
        }
    }

    protected abstract Tuple4<String, Integer, ArrayList<Tuple2<String, Long>>, C> processExample(Example example, C classifier);

    protected abstract C createClassifier();

    protected String produceMessage(C classifier, String timestamp, int predicted, int exampleClass, ArrayList<Tuple2<String, Long>> performances) throws IOException {
        String result = "classifierResult";
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_NAME, name);
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_PARAMS, classifier.generateClassifierParams());
        result += "," + produceTag(BaseClassifierTags.JOB_ID, getRuntimeContext().getJobId().toString());
        result += "," + produceTag(BaseClassifierTags.DATASET, dataset);
        result += "," + produceTag(BaseClassifierTags.CLASS, exampleClass);
        result += "," + produceTag(BaseClassifierTags.PREDICTED, predicted) + " ";

        result += performances.stream().map(entry -> produceFieldAsNumber(entry.f0, entry.f1, "i")).collect(Collectors.joining(",")) + " ";

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
        ValueStateDescriptor<Long> sampleNumberDescriptor = new ValueStateDescriptor<>("sampleNumber", TypeInformation.of(new TypeHint<Long>() {
        }));
        sampleNumberState = getRuntimeContext().getState(sampleNumberDescriptor);
    }

    protected abstract void registerClassifier(); //its abstract because TypeInfo cannot be templated
}
