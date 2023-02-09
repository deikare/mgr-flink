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

import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import vfdt.hoeffding.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.BiFunction;

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
    public static Tuple2<LinkedList<Example>, HashSet<String>> readExamples(String filepath) throws FileNotFoundException {
        HashSet<String> attributes = new HashSet<>();
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
        return new Tuple2<>(examples, attributes);
    }

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
//        env.getConfig().enableForceAvro();

        final String filepath = "/home/deikare/wut/streaming-datasets/" + "elec.csv";

        Tuple2<LinkedList<Example>, HashSet<String>> data = readExamples(filepath);

        env.fromCollection(data.f0).process(new VFDT());

//        env.fromSource()

        //TODO exporter from csv to example class
//        CsvReaderFormat<Example> csvReaderFormat = CsvReaderFormat.forSchema()

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
