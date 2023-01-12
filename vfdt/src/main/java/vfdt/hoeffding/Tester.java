package vfdt.hoeffding;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Tester {

    public static void main(String[] args) {

        String path = "/home/deikare/wut/streaming-datasets/" + "elec.csv";
        HashSet<String> attributes = new HashSet<>();

        try {
            File file = new File(path);
            Scanner scanner = new Scanner(file);

            String line = scanner.nextLine();

            String[] attributesAsString = line.split(",");
            System.out.println(attributesAsString[attributesAsString.length - 1]);
            int n = attributesAsString.length - 1;
            for (int i = 0; i < n; i++) {
                attributes.add(attributesAsString[i]);
            }

            SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);
            int R = 1;
            double delta = 0.05;
            double tau = 0.1;
            long nMin = 10;

            HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>(R, delta, attributes, tau, nMin, statisticsBuilder, (String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) -> {
                return 0.0;
            });

            while (scanner.hasNext()) {
                line = scanner.nextLine();

                String[] attributesValuesAsString = line.split(",");
                HashMap<String, Double> attributesMap = new HashMap<>(n);
                for (int i = 0; i < n; i++) {
                    attributesMap.put(attributesAsString[i], Double.parseDouble(attributesValuesAsString[i]));
                }

                String className = attributesValuesAsString[n];
                Example example = new Example(className, attributesMap);
                tree.train(example);
            }


        } catch (FileNotFoundException e) {
            System.out.println("No file found");
            e.printStackTrace();
        }


    }
}
