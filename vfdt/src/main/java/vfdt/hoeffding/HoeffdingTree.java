package vfdt.hoeffding;

import org.apache.flink.api.java.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

/*TODO etap 2
- przerzucić liczenie statystyk do grafany - process function zwraca jsona z zmierzonymi statystykami
- dorzucić grafanę (lub inne narzędzie UI) robiącą wykres dokładności w czasie rzeczywistym
- dopisać do pracy mgr
- zapuścić na innych danych
- poczytać o klasyfikatorach bayesowskich
sdasdsd*/

/*
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

//Parametry:

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

public abstract class HoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends BaseClassifier {
    private final Logger logger = LoggerFactory.getLogger(HoeffdingTree.class);
    private final long nMin;

    private final long batchStatLength;
    private final int R;
    private final double delta;
    private final HashSet<String> attributes; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private final double tau;
    private final Node<N_S, B> root;
    private final B statisticsBuilder;

//    private final AllTreeStatistics treeStatistics;

    private long n = 0L;


    public HoeffdingTree(long classesNumber, double delta, HashSet<String> attributes, double tau, long nMin, B statisticsBuilder, long batchStatLength) {
        this.R = (int) (Math.log(classesNumber) / Math.log(2));
        this.delta = delta;
        this.attributes = attributes;
        this.tau = tau;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.root = new Node<>(statisticsBuilder);
        this.batchStatLength = batchStatLength;
//        treeStatistics = new AllTreeStatistics(batchStatLength);
    }

    protected Tuple2<String, HashMap<String, Long>> classifyImplementation(Example example, HashMap<String, Long> performances) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        String predictedClass = leaf.getMajorityClass();
        logger.info(example + " predicted with " + predictedClass);
        return new Tuple2<>(predictedClass, performances);
    }

    @Override
    public String generateClassifierParams() {
        return "r" + R + "_d" + delta + "_t" + tau + "_n" + nMin;
    }

    protected HashMap<String, Long> trainImplementation(Example example) throws RuntimeException {
        n++;
        HashMap<String, Long> trainingPerformances = new HashMap<>();
        Node<N_S, B> leaf = getLeaf(example, trainingPerformances);
        logger.info("Training: " + example.toString());

        leaf.updateStatistics(example);

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
//                treeStatistics.updateOnNodeSplit(false);
                leaf.split(pojo.attribute, statisticsBuilder, example);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                leaf.split(pojo.attribute, statisticsBuilder, example);
//                treeStatistics.updateOnNodeSplit(true);
            } else logger.info("No split");
        } else logger.info("Not enough samples to test splits");

        logger.info(leaf.getStatistics().toString());

        return trainingPerformances;
    }

    private Node<N_S, B> getLeaf(Example example, HashMap<String, Long> performances) {
        Instant start = Instant.now();
        long count = 1;
        Node<N_S, B> result = root;
        while (!(result.isLeaf())) {
            count++;
            result = result.getChild(example);
        }
        performances.put(HoeffdingTreeFields.NODES_DURING_TRAVERSAL_COUNT, count);
        performances.put(HoeffdingTreeFields.DURING_TRAVERSAL_DURATION, toNow(start));
        return result;
    }

    private Node<N_S, B> getLeaf(Example example) {
        Node<N_S, B> result = root;
        while (!(result.isLeaf()))
            result = result.getChild(example);

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
                double h = heuristic(attribute, node);
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

    protected abstract double heuristic(String attribute, Node<N_S, B> node);

//    public String printStatistics() {
//        return treeStatistics.totalStatisticsToString();
//    }
//
//    public String getSimpleStatistics() {
//        return treeStatistics.toStringSimple();
//    }

    public void printStatisticsToFile(String dataPath) throws FileNotFoundException, UnsupportedEncodingException {
//stat_data_r._d._t._n._b
        Path path = Paths.get(dataPath);
        String dataFileName = path.getFileName().toString();
        String dataFileNameNoExtension = dataFileName.substring(0, dataFileName.lastIndexOf('.'));
        String statFileName = "stat_" + dataFileNameNoExtension + "_r" + R + "_d" + delta + "_t" + tau + "_n" + nMin + "_b" + batchStatLength + ".txt";
        String statFilePath = System.getenv("MGR_FLINK_RESULTS_PATH") + "/" + statFileName;
        PrintWriter writer = new PrintWriter(statFilePath, "UTF-8");
//        writer.write(new Gson().toJson(treeStatistics));
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


            HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>(classesAmount, delta, attributes, tau, nMin, statisticsBuilder, batchStatLength) {
                @Override
                protected double heuristic(String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) {
                    return 0;
                }
            };

            while (scanner.hasNext()) {
                line = scanner.nextLine();

                String[] attributesValuesAsString = line.split(",");
                HashMap<String, Double> attributesMap = new HashMap<>(n);
                for (int i = 0; i < n; i++) {
                    attributesMap.put(attributesAsString[i], Double.parseDouble(attributesValuesAsString[i]));
                }

                String className = attributesValuesAsString[n];
                Example example = new Example(className, attributesMap);

                tree.trainImplementation(example);

//                if (tree.classifyImplementation(example) == null)
//                    tree.classifyImplementation(example); //TODO spytać o to, czy najpierw powinna być predykcja, czy trening
            }

//            System.out.println(tree.printStatistics());
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
