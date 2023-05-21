package vfdt;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import vfdt.hoeffding.BaseClassifier;
import vfdt.hoeffding.BaseClassifierTags;
import vfdt.hoeffding.Example;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

public abstract class BaseProcessFunction<C extends BaseClassifier> extends KeyedProcessFunction<Long, Example, String> {
    protected transient ValueState<C> classifierState;
    protected transient ValueState<String> nameState;
    protected transient ValueState<String> experimentIdState;
    protected transient ValueState<String> datasetState;

    protected abstract C getClassifier();

    @Override
    public void processElement(Example example, KeyedProcessFunction<Long, Example, String>.Context context, Collector<String> collector) throws Exception {
        C classifier = classifierState.value();
        if (classifier == null)
            classifier = getClassifier();

        Tuple2<Long, HashMap<String, Long>> trainingResult = classifier.train(example);
        Tuple2<String, HashMap<String, Long>> classifyResult = classifier.classify(example, trainingResult.f1);

        String msg = produceMessage(trainingResult.f0, classifyResult, example.getClassName());

        collector.collect(msg);
        classifierState.update(classifier);
    }

    private String produceMessage(Long timestamp, Tuple2<String, HashMap<String, Long>> classifyResult, String exampleClass) throws IOException {
        String result = nameState.value();
        result += "," + produceTag(BaseClassifierTags.CLASSIFIER_PARAMS, classifierState.value().generateClassifierParams());
        result += "," + produceTag(BaseClassifierTags.EXPERIMENT_ID, experimentIdState.value());
        result += "," + produceTag(BaseClassifierTags.DATASET, datasetState.value());
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
        super.open(parameters);

//        TypeInformation<C> classifierInfo = TypeInformation.of(new TypeHint<C>() { //todo exception here - try to put this line as lambda in main
//        });
//        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classifier", classifierInfo));
//        if (classifierState.value() == null)
//            classifierState.update(createClassifierFunction.get());


/*        ValueStateDescriptor<String> nameDescriptor = new ValueStateDescriptor<>(BaseClassifierTags.CLASSIFIER_NAME, Types.STRING);
        nameState = getRuntimeContext().getState(nameDescriptor);
        if (nameState.value() == null) {
            ConfigOption<String> nameConfig = ConfigOptions
                    .key(BaseClassifierTags.CLASSIFIER_NAME)
                    .stringType()
                    .noDefaultValue();
            nameState.update(parameters.getString(nameConfig));
        }*/
        nameState = initializeState(BaseClassifierTags.CLASSIFIER_NAME, parameters);
        /*ValueStateDescriptor<String> experimentIdDescriptor = new ValueStateDescriptor<>(BaseClassifierTags.EXPERIMENT_ID, Types.STRING);
        experimentIdState = getRuntimeContext().getState(experimentIdDescriptor);
        if (experimentIdState.value() == null)
            experimentIdState.update(UUID.randomUUID().toString());*/
//        experimentIdState = initializeState(BaseClassifierTags.EXPERIMENT_ID, UUID.randomUUID().toString());

        /*ValueStateDescriptor<String> datasetDescriptor = new ValueStateDescriptor<>(BaseClassifierTags.DATASET, Types.STRING);
        datasetState = getRuntimeContext().getState(datasetDescriptor);
        if (datasetState.value() == null) {
            ConfigOption<String> datasetConfig = ConfigOptions
                    .key(BaseClassifierTags.DATASET)
                    .stringType()
                    .noDefaultValue();
            datasetState.update(parameters.getString(datasetConfig));
        }*/
        datasetState = initializeState(BaseClassifierTags.DATASET, parameters);

    }

    private ValueState<String> initializeState(String name, String defaultValue) throws IOException {
        ValueStateDescriptor<String> stateDescriptor = new ValueStateDescriptor<>(name, Types.STRING);
        ValueState<String> valueState = getRuntimeContext().getState(stateDescriptor);
        if (valueState.value() == null)
            valueState.update(defaultValue);

        return valueState;
    }

    private ValueState<String> initializeState(String name, Configuration parameters) throws IOException {
        ValueStateDescriptor<String> stateDescriptor = new ValueStateDescriptor<>(name, Types.STRING);
        ValueState<String> valueState = getRuntimeContext().getState(stateDescriptor);
        if (valueState.value() == null) {
            ConfigOption<String> configOption = ConfigOptions
                    .key(name)
                    .stringType()
                    .noDefaultValue();
            datasetState.update(parameters.getString(configOption));
        }
        return valueState;
    }

}
