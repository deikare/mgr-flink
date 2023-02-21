package vfdt.hoeffding;

import com.google.gson.Gson;
import org.apache.flink.api.java.tuple.Tuple3;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private long batchStatLength;
    private int R;
    private double delta;
    private HashSet<String> attributes; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private double tau;
    private Node<N_S, B> root;
    private B statisticsBuilder;
    private SerializableHeuristic<N_S, B> heuristic;

    private AllTreeStatistics treeStatistics;


    public HoeffdingTree() {
    }

    public HoeffdingTree(long classesNumber, double delta, HashSet<String> attributes, double tau, long nMin, B statisticsBuilder, SerializableHeuristic<N_S, B> heuristic, long batchStatLength) {
        this.R = (int) (Math.log(classesNumber) / Math.log(2));
        this.delta = delta;
        this.attributes = attributes;
        this.tau = tau;
        this.n = 0;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.heuristic = heuristic;
        this.root = new Node<>(statisticsBuilder);
        this.batchStatLength = batchStatLength;
        treeStatistics = new AllTreeStatistics(batchStatLength);
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
            leaf.resetN();
            double eps = getEpsilon();

            HighestHeuristicPOJO pojo = new HighestHeuristicPOJO(leaf);

            if (pojo.attribute == null) {
                String msg = "Hoeffding test showed no attributes";
                logger.info(msg);
                throw new RuntimeException(msg);
            } else if (pojo.hXa != null && pojo.hXb != null && (pojo.hXa - pojo.hXb > eps)) {
                logger.info("Heuristic value is correspondingly higher, splitting");
                treeStatistics.updateOnNodeSplit(false);
                leaf.split(pojo.attribute, statisticsBuilder);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                leaf.split(pojo.attribute, statisticsBuilder);
                treeStatistics.updateOnNodeSplit(true);
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

    public String printStatistics() {
        return treeStatistics.totalStatisticsToString();
    }

    public void printStatisticsToFile(String dataPath) throws FileNotFoundException, UnsupportedEncodingException {
//stat_data_r._d._t._n._b
        Path path = Paths.get(dataPath);
        String dataFileName = path.getFileName().toString();
        String dataFileNameNoExtension = dataFileName.substring(0, dataFileName.lastIndexOf('.'));
        String statFileName = "stat_" + dataFileNameNoExtension + "_r" + R + "_d" + delta + "_t" + tau + "_n" + nMin + "_b" + batchStatLength + ".txt";
        String statFilePath = System.getenv("MGR_FLINK_RESULTS_PATH") + "/" + statFileName;
        PrintWriter writer = new PrintWriter(statFilePath, "UTF-8");
        writer.write(new Gson().toJson(treeStatistics));
        writer.close();

        System.out.println("Printing STAT to file: " + statFileName);
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
            double delta = 0.05;
            double tau = 0.2;
            long nMin = 50;
            long batchStatLength = 500;
            long classesAmount = 2;

            SerializableHeuristic<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> heuristic = (s, node) -> {
                double threshold = 0.5;
                return Math.abs(threshold - node.getStatistics().getSplittingValue(s)) / threshold;
            };

//            heuristic = (s, node) -> 0.0;

            HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<>(classesAmount, delta, attributes, tau, nMin, statisticsBuilder, heuristic, batchStatLength);

            while (scanner.hasNext()) {
                line = scanner.nextLine();

                String[] attributesValuesAsString = line.split(",");
                HashMap<String, Double> attributesMap = new HashMap<>(n);
                for (int i = 0; i < n; i++) {
                    attributesMap.put(attributesAsString[i], Double.parseDouble(attributesValuesAsString[i]));
                }

                String className = attributesValuesAsString[n];
                Example example = new Example(className, attributesMap);
                tree.predict(example);
                tree.train(example);
            }

            System.out.println(tree.printStatistics());
//            tree.printStatisticsToFile(path);
        } catch (FileNotFoundException e) {
            System.out.println("No file found");
            e.printStackTrace();
        }
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException(e);
//        }
    }
}
