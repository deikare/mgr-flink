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
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import vfdt.hoeffding.Example;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

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
    public static Tuple2<LinkedList<Example>, String> readExamples(String filepath) throws FileNotFoundException {
        LinkedList<String> attributes = new LinkedList<>();
        LinkedList<Example> examples = new LinkedList<>();

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
                HashMap<String, Double> attributesMap = new HashMap<>(n);
                for (int i = 0; i < n; i++) {
                    attributesMap.put(attributesAsString[i], Double.parseDouble(attributeValuesAsString[i]));
                }

                String className = attributeValuesAsString[n];
                examples.add(new Example(className, attributesMap));
            }
        } catch (FileNotFoundException | NumberFormatException e) {
            throw new RuntimeException(e);
        }
        return new Tuple2<>(examples, String.join(",", attributes));
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

        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        final String filepath = "/home/deikare/wut/streaming-datasets/" + "elec.csv";

        Tuple2<LinkedList<Example>, String> data = readExamples(filepath);

        double delta = 0.05;
        double tau = 0.2;
        long nMin = 50;
        long batchStatLength = 500;
        long classesAmount = 2;
        ParameterTool params = getVFDTOptions(classesAmount, delta, data.f1, tau, nMin, batchStatLength);
        env.getConfig().setGlobalJobParameters(params);

        KafkaSink<String> kafkaSink = KafkaSink.<String>builder()
                .setBootstrapServers("localhost:9092")
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("simple-vfdt")
                        .setValueSerializationSchema(new SimpleStringSchema())
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .build();

        DataStream<String> stream = env.fromCollection(data.f0)
                .keyBy(Example::getId)
                .process(new VfdtProcessFunction()).name("process-examples");

        stream.addSink(new LoggingSink()).name("logging-sink");
        stream.sinkTo(kafkaSink).name("kafka-sink");

        stream.print("std-out-sink");



        /*
         * Here, you can start creating your execution plan for Flink.
         *
         * Start with getting some data from the environment, like
         * 	env.fromSequence(1, 10);
         *
         * then, transform the resulting DataStream<Long> using operations
         * like
         * 	.filter()
         * 	.flatMap()
         * 	.window()
         * 	.process()
         *
         * and many more.
         * Have a look at the programming guide:
         *
         * https://nightlies.apache.org/flink/flink-docs-stable/
         *
         */

        // Execute program, beginning computation.
        env.execute("Flink Java API Skeleton");
    }
}
