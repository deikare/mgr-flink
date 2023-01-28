package vfdt.hoeffding;

import org.apache.flink.api.java.tuple.Tuple3;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
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

//TODO miary wydajności:
 /*
 histogramy:
- średnia szybkość przeglądania drzewa
- liczba node'ów w drzewie
- ile średnio node'ów musi pokonać próbka podczas przejścia
- średni czas przetworzenia próbki podczas uczenia
- średni czas przetworzenia próbki podczas klasyfikacji
- szybkość obliczenia klasy większościowej
- liczba próbek poprawnie sklasyfikowanych
- liczba próbek błędnie sklasyfikowanych
 */

public class HoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> {
    private Logger logger = Logger.getLogger(HoeffdingTree.class.getName());
    private long n; //TODO - figure out
    private long nMin;
    private int R;
    private double delta;
    private HashSet<String> attributes; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private double tau;
    private Node<N_S, B> root;
    private B statisticsBuilder;
    private BiFunction<String, Node<N_S, B>, Double> heuristic;

    private TreeStatistics treeStatistics;


    public HoeffdingTree() {
    }

    public HoeffdingTree(int R, double delta, HashSet<String> attributes, double tau, long nMin, B statisticsBuilder, BiFunction<String, Node<N_S, B>, Double> heuristic, long batchStatLength) {
        this.R = R;
        this.delta = delta;
        this.attributes = attributes;
        this.tau = tau;
        this.n = 0;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.heuristic = heuristic;
        this.root = new Node<>(statisticsBuilder);
        treeStatistics = new TreeStatistics(batchStatLength);
    }

    public String predict(Example example) throws RuntimeException {
        Instant start = Instant.now();
        Tuple3<Node<N_S, B>, Long, Long> leafWithTimes = getLeaf(example);
        Node<N_S, B> leaf = leafWithTimes.f0;
        String predictedClass = leaf.getMajorityClass();
        long duration = Duration.between(start, Instant.now()).toNanos();
        treeStatistics.updateOnClassification(leafWithTimes.f1, leafWithTimes.f2, duration, Objects.equals(predictedClass, example.getClassName()));
        logger.info(example + " predicted with " + predictedClass);
        return predictedClass;
    }

    public void train(Example example) throws RuntimeException {
        Instant start = Instant.now();
        Tuple3<Node<N_S, B>, Long, Long> leafWithTimes = getLeaf(example);
        Node<N_S, B> leaf = leafWithTimes.f0;
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
                treeStatistics.updateOnNodeSplit();
                leaf.split(pojo.attribute, statisticsBuilder);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                leaf.split(pojo.attribute, statisticsBuilder);
                treeStatistics.updateOnNodeSplit();
            } else logger.info("No split");
        } else logger.info("Not enough samples to test splits");

        long duration = Duration.between(start, Instant.now()).toNanos();
        treeStatistics.updateOnLearning(leafWithTimes.f1, leafWithTimes.f2, duration);

        logger.info(leaf.getStatistics().toString());
    }

    private Tuple3<Node<N_S, B>, Long, Long> getLeaf(Example example) {
        Instant start = Instant.now();
        long count = 1;
        Node<N_S, B> result = root;
        while (!(result.isLeaf())) {
            count++;
            result = result.getChild(example);
        }
        return new Tuple3<>(result, Duration.between(start, Instant.now()).toNanos(), count);
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

    public String getStatistics() {
        return treeStatistics.toString();
    }


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
            attributes.addAll(Arrays.asList(attributesAsString).subList(0, n));

            SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);
            int R = 1;
            double delta = 0.05;
            double tau = 0.1;
            long nMin = 2;
            long batchStatLength = 1000;

            HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>(R, delta, attributes, tau, nMin, statisticsBuilder, (String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) -> {
                return 0.0;
            }, batchStatLength);

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

            System.out.println(tree.getStatistics());


        } catch (FileNotFoundException e) {
            System.out.println("No file found");
            e.printStackTrace();
        }
    }
}
