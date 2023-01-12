package vfdt.hoeffding;

import java.util.HashSet;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Logger;


/*
TODO lista pytań:
- TODO !!! najważniejsze - czy update statystyk nie powinien być zawsze, niezależnie od klasy większościowej?
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

TODO!!! zrobić statystyki i wrzucić do sprawka
TODO - naprawić podział na klasyfikację i na trening
 */

//Parametry:

//TODO check if B can be replaced by static method in NodeStatistics class
public class HoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> {
    private Logger logger = Logger.getLogger(HoeffdingTree.class.getName());
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

    public String predict(Example example) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        String predictedClass = leaf.getMajorityClass();
        logger.info(example.toString() + "predicted with " + predictedClass);
        return predictedClass;
    }

    public void train(Example example) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        logger.info("Training: " + example.toString());

        leaf.updateStatistics(example);
        n++;

        if (leaf.getN() > nMin) {
            double eps = getEpsilon();

            HighestHeuristicPOJO pojo = new HighestHeuristicPOJO(leaf);

            if (pojo.attribute == null) {
                String msg = "Hoeffding test showed no attributes";
                logger.info(msg);
                throw new RuntimeException(msg);
            } else if (pojo.hXa != null && pojo.hXb != null && (pojo.hXa - pojo.hXb > eps)) {
                logger.info("Heuristic value is correspondingly higher, splitting");
                leaf.split(pojo.attribute, statisticsBuilder);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                leaf.split(pojo.attribute, statisticsBuilder);
            } else logger.info("No split");
        } else logger.info("Not enough samples to test splits");

        logger.info(leaf.getStatistics().toString());

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

    private class HighestHeuristicPOJO {
        final String attribute;
        final Double hXa;
        final Double hXb;

        public HighestHeuristicPOJO(Node<N_S, B> node) {
            Double hXbTemp = null;
            Double hXaTemp = null;
            String xaTemp = null;
            String xbTemp = null;

            for (String attribute : attributes) {
                Double h = heuristic.apply(attribute, node);
                if (xaTemp == null || h > hXaTemp) {
                    xaTemp = attribute;
                    hXaTemp = h;
                } else if (xbTemp == null || h > hXbTemp) {
                    xbTemp = attribute;
                    hXbTemp = h;
                }
            }

            hXb = hXbTemp;
            hXa = hXaTemp;
            attribute = xaTemp;
        }
    }


}
