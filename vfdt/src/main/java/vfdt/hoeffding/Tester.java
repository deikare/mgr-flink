package vfdt.hoeffding;

import java.util.HashSet;

public class Tester {

    public static void main(String[] args) {

        HashSet<String> classNames = new HashSet<>();
        classNames.add("car");
        classNames.add("bike");

        HashSet<String> attributes = new HashSet<>();
        attributes.add("x");
        attributes.add("y");

        SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(classNames, attributes);
        int R = 1;
        double delta = 0.05;
        double tau = 0.1;
        long nMin = 1000;

        HoeffdingTree<NodeWithAttributeValueCountsStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>(R, delta, attributes, tau, nMin, statisticsBuilder, (String attribute, Node<NodeWithAttributeValueCountsStatistics, SimpleNodeStatisticsBuilder> node) -> {
            return 0.0;
        });
    }
}