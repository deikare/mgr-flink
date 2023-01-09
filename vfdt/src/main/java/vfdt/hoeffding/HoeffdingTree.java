package vfdt.hoeffding;

import org.apache.flink.api.java.tuple.Tuple4;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.BiFunction;


/*
TODO lista pytań:
- kwestia implementacji w Flinku - czy zrobić po prostu obiekt drzewa przy użyciu ValueState<Drzewo>, potem zrównoleglanie z wykorzystaniem Flinka

- kwestia klasyfikatora w liściu drzewa - czy zostawić statystycznie obliczaną klasę większościową,
czy od razu robić np klasyfikator bayesowski - w następnym etapie

- podział atrybutów na ciągłe i dyskretne - jak ustalać wartość progową podziału (np z wykorzystaniem B-drzewa z przykładu z książki), jak w przypadku atrybutów dyskretnych - nawet w przypadku dyskretnych sprawdzamy nierówności
- spróbować na początku najłatwiejszą
- dyskretne zmienne zawsze można kodować jako ciągłe

- zrobić start N próbek, podczas których tylko uczymy klasyfikator

- podział atrybutów - czy robić zawsze podział na dwie gałęzie
    - czy w takiej sytuacji przy zmiennych dyskretnych robić prosty podział - gałęzie równe/różne atrybutowi o największej heurystyce podczas dzielenia

- czy kształt funkcji heurystyki atrybutów powinien być zależny od node'a w drzewie, czy stały dla całego drzewa -
    funkcja będzie wtedy liczyć heurystykę tylko na podstawie statystyk node'a w drzewie - na początku stałą, potem można rozbudować

- jaką użyć sensowną funkcję heurystyki do testowania prostego klasyfikatora

- jaki zrobić prosty przykład do testowania - np. atrybuty ((ciągłe)x, y, jakieś parametry dyskretne)

- czy jest jakaś praca opisująca dobór parametru tau - eksperymentalnie - zależne od problemu, charakteru danych - użyc w klasyfikatorze tau

- czy robić coś więcej podczas uczenia w przypadku, gdy klasa aktualnej próbki == klasa większościowa liścia? np prowadzić statystykę poprawnych trafień

- czy na ten moment wystarczy odczytywanie strumienia z pliku, czy podpiąć Flinka pod Kafkę odczytującą z wielu producentów

- co napisać w podsumowaniu pracowni - 10-15 stron - spróbować pisać tekst ściśle pod pracę:
    - cel pracy
    - opisać Flinka
    - algorytm
    - eksperymenty
 */

public class HoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> {
    private long n;
    private long nMin;
    private int R;
    private double delta;
    private HashSet<String> attributes; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private double tau;
    private Node<N_S, B> root;
    private B statisticsBuilder;
    private BiFunction<String, Node<N_S, B>, Double> heuristic;


    public HoeffdingTree() {
    }

    public HoeffdingTree(int R, double delta, HashSet<String> attributes, double tau, long nMin, B statisticsBuilder, BiFunction<String, Node<N_S, B>, Double> heuristic) {
        this.R = R;
        this.delta = delta;
        this.attributes = attributes;
        this.tau = tau;
        this.n = 0;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.heuristic = heuristic;
        this.root = new Node<>(statisticsBuilder);
    }

    public void train(Example example) {
        Node<N_S, B> leaf = getLeaf(example);

        String exampleClass = example.getClassName();
        if (Objects.equals(exampleClass, leaf.getMajorityClass())) {
            //TODO
        } else {
            leaf.updateStatistics(example);
            n++;

            if (leaf.getStatistics().getN() > nMin) {
                Tuple4<String, Double, String, Double> tuple4 = twoAttributesWithLargestHeuristic(leaf);

                double eps = getEpsilon();
                String f0 = tuple4.f0;
                Double f1 = tuple4.f1;
                Double f3 = tuple4.f3;

                if (f1 != null && f3 != null && f1 - f3 > eps) {
                    leaf.splitLeaf(f0, statisticsBuilder);
                } else if (eps < tau) {
                    leaf.splitLeaf(f0, statisticsBuilder);
                }
            }
        }
    }

    private Node<N_S, B> getLeaf(Example example) {
        Node<N_S, B> result = root;
        while (!(result.isLeaf())) {
            result = result.getChild(example);
        }
        return result;
    }

    private double getEpsilon() {
        return Math.sqrt(Math.pow(R, 2) * Math.log(2 / delta) / (2 * n));
    }

    private Tuple4<String, Double, String, Double> twoAttributesWithLargestHeuristic(Node<N_S, B> node) {
        String xa = null;
        String xb = null;
        Double hXa = null;
        Double hXb = null;
        for (String attribute : attributes) {
            Double h = heuristic.apply(attribute, node);
            if (xa == null || h > hXa) {
                xa = attribute;
                hXa = h;
            } else if (xb == null || h > hXb) {
                xb = attribute;
                hXb = h;
            }
        }

        return new Tuple4<>(xa, hXa, xb, hXb);
    }


}
