package vfdt.hoeffding;

import org.apache.flink.api.java.tuple.Tuple4;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class HoeffdingTree <K, S extends NodeStatistics<K>, C extends ComparatorInterface<K>, B extends StatisticsBuilderInterface<K, S>> {
    private long n;
    private long nMin;
    private int R;
    private double delta;
    private HashSet<String> attributes; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private HashSet<String> classLabels;
    private double tau;
    private Node<K, S, C> root;
    private B statisticsBuilder;
    private BiFunction<String, Node<K, S, C>, Double> heuristic;

    private BiConsumer<Example<K>, Node<K, S, C>> nodeSplitter; //TODO consider using attribute instead of example

    public HoeffdingTree() {
    }

    public HoeffdingTree(int R, double delta, HashSet<String> attributes, HashSet<String> classLabels, double tau, long nMin, B statisticsBuilder, BiFunction<String, Node<K, S, C>, Double> heuristic, BiConsumer<Example<K>, Node<K, S, C>> nodeSplitter) {
        this.R = R;
        this.delta = delta;
        this.attributes = attributes;
        this.classLabels = classLabels;
        this.tau = tau;
        this.nodeSplitter = nodeSplitter;
        this.n = 0;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.heuristic = heuristic;
        this.root = new Node<>(statisticsBuilder.build(classLabels));
    }

    public void train(Example<K> example) {
        Node<K, S, C> leaf = getLeaf(example);

        String exampleClass = example.getClassName();
        if (Objects.equals(exampleClass, leaf.getMajorityClass())) {
            //TODO
        }
        else {
            leaf.getStatistics().update(example);
            n += 1;

            if (leaf.getStatistics().getN() > nMin) {
                Tuple4<String, Double, String, Double> tuple4 = twoAttributesWithLargestHeuristic(leaf);

                double eps = getEpsilon();
                if (tuple4.f1 != null && tuple4.f3 != null && tuple4.f1 - tuple4.f3 > eps) {
                    nodeSplitter.accept(example, leaf);
                    //split
                    //init new leafs
                }
                else if (eps < tau) {
                    nodeSplitter.accept(example, leaf);
                    //split
                    //init new leafs
                }
            }
        }
    }

    private Node<K, S, C> getLeaf(Example<K> example) {
        Node<K, S, C> result = root;
        while (!(result.isLeaf())) {
            result = result.getChild(example);
        }
        return result;
    }

    private double getEpsilon() {
        return Math.sqrt(Math.pow(R, 2) * Math.log(2/delta) / (2 * n));
    }

    private Tuple4<String, Double, String, Double> twoAttributesWithLargestHeuristic(Node<K, S, C> node) {
        String xa = null;
        String xb = null;
        Double hXa = null;
        Double hXb = null;
        for (String attribute : attributes) {
            Double h = heuristic.apply(attribute, node);
            if (xa == null || h > hXa)
            {
                xa = attribute;
                hXa = h;
            }
            else if (xb == null || h > hXb) {
                xb = attribute;
                hXb = h;
            }
        }

        return new Tuple4<>(xa, hXa, xb, hXb);
    }


}
