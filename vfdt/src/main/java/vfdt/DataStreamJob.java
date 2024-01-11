/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package vfdt;

import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import vfdt.classifiers.base.BaseClassifierTags;
import vfdt.classifiers.dwm.classic.DynamicWeightedMajority;
import vfdt.classifiers.dwm.classifiers.bayes.naive.GaussianNaiveBayesClassifier;
import vfdt.classifiers.hoeffding.HoeffdingTree;
import vfdt.classifiers.hoeffding.Node;
import vfdt.classifiers.hoeffding.SimpleNodeStatistics;
import vfdt.classifiers.hoeffding.SimpleNodeStatisticsBuilder;
import vfdt.inputs.Example;
import vfdt.processors.coding.Encoder;
import vfdt.processors.dwm.ClassicDwmProcessFunction;
import vfdt.processors.hoeffding.VfdtProcessFunction;
import vfdt.sinks.LoggingSink;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class DataStreamJob {
    public static Tuple3<LinkedList<Example>, HashSet<String>, HashMap<Integer, String>> readExamples(String filepath) throws FileNotFoundException {

        LinkedList<String> attributes = new LinkedList<>();
        LinkedList<Example> examples = new LinkedList<>();
        Encoder encoder = new Encoder();

        try {
            File file = new File(filepath);

            Scanner scanner = new Scanner(file);

            String line = scanner.nextLine();

            String[] attributesAsString = line.split(",");
            int n = attributesAsString.length - 1;
            attributes.addAll(Arrays.asList(attributesAsString).subList(0, n));

            while (scanner.hasNext()) {
                line = scanner.nextLine();

                String[] attributeValuesAsString = line.split(",");
                double[] attributesValues = new double[n];
                for (int i = 0; i < n; i++) {
                    attributesValues[i] = Double.parseDouble(attributeValuesAsString[i]);
                }

                //if encoding/decoding needed - add decoder in keyedProcessFunction
//                String className = attributeValuesAsString[n];
//                examples.add(new Example(encoder.encode(className), attributesValues));

                encoder.encode(attributeValuesAsString[n]);
                int className = Integer.parseInt(attributeValuesAsString[n]);
                examples.add(new Example(className, attributesValues));
            }
        } catch (FileNotFoundException | NumberFormatException e) {
            throw new RuntimeException(e);
        }
        return new Tuple3<>(examples, new HashSet<>(attributes), encoder.decoder());
    }

    public static ParameterTool getVFDTOptions(long classesNumber, double delta, String attributes, double tau, long nMin, long batchStatLength) {
        HashMap<String, String> options = new HashMap<>();

        options.put("classesNumber", String.valueOf(classesNumber));
        options.put("delta", String.valueOf(delta));
        options.put("attributes", attributes);
        options.put("tau", String.valueOf(tau));
        options.put("nMin", String.valueOf(nMin));
        options.put("batchStatLength", String.valueOf(batchStatLength));

        return ParameterTool.fromMap(options);
    }

    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        final String dataset = "elec";
        final String filepath = "/home/deikare/wut/streaming-datasets/" + dataset + ".csv";

        Tuple3<LinkedList<Example>, HashSet<String>, HashMap<Integer, String>> data = readExamples(filepath);
        HashSet<String> attributes = data.f1; //protects from TupleSerialization error!
        HashMap<Integer, String> decoder = data.f2;

        HashMap<String, String> options = new HashMap<>();

        options.put(BaseClassifierTags.CLASSIFIER_NAME, "vfdt");
        options.put(BaseClassifierTags.DATASET, dataset);

        env.getConfig().setGlobalJobParameters(ParameterTool.fromMap(options));


        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("classifier-performances")

                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        DataStream<String> vfdtStream = env.fromCollection(data.f0)
                .keyBy(Example::getId)
                .process(new VfdtProcessFunction("vfdt", dataset) {
                    @Override
                    protected HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> createClassifier() {
                        double delta = 0.05;
                        double tau = 0.2;
                        long nMin = 50;
                        long classesAmount = decoder.size();

                        SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes.size());
                        return new HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>(classesAmount, delta, attributes.size(), tau, nMin, statisticsBuilder) {
                            @Override
                            protected double heuristic(int attributeNumber, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) {
                                double threshold = 0.5;
                                return Math.abs(threshold - node.getStatistics().getSplittingValue(attributeNumber)) / threshold;
                            }
                        };
                    }
                })
                .name("process-examples-vfdt");


        vfdtStream.addSink(new LoggingSink()).name("logging-sink-vfdt");
        vfdtStream.sinkTo(kafkaSink).name("kafka-sink-vfdt");

        DataStream<String> dwmStream = env.fromCollection(data.f0)
                .keyBy(Example::getId)
                .process(new ClassicDwmProcessFunction("classicDwm", dataset) {
                    @Override
                    protected DynamicWeightedMajority<GaussianNaiveBayesClassifier> createClassifier() {
                        int classNumber = decoder.size();
                        double beta = 0.5;
                        double threshold = 0.4;
                        int updateClassifiersEachSamples = 20;
                        return new DynamicWeightedMajority<GaussianNaiveBayesClassifier>(beta, threshold, classNumber, updateClassifiersEachSamples) {
                            @Override
                            protected GaussianNaiveBayesClassifier createClassifier() {
                                return new GaussianNaiveBayesClassifier(classNumber, attributes.size());
                            }
                        };
                    }
                }).name("process-examples-dwm");


        dwmStream.addSink(new LoggingSink()).name("logging-sink-dwm");
        dwmStream.sinkTo(kafkaSink).name("kafka-sink-dwm");

        env.execute("Flink Java API Skeleton");
    }
}
