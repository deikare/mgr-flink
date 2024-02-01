package vfdt.classifiers.hoeffding;

import org.apache.flink.api.java.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vfdt.classifiers.base.BaseClassifierTrainAndClassify;
import vfdt.classifiers.helpers.Helpers;
import vfdt.inputs.Example;

import java.time.Instant;
import java.util.*;

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

public abstract class HoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends BaseClassifierTrainAndClassify {
    private final Logger logger = LoggerFactory.getLogger(HoeffdingTree.class);
    private final long nMin;

    private final int R;
    private final double delta;
    private final int attributesNumber; //TODO separate attributes on those which are continuous and discrete - this way e.g. decorator pattern should be used in branching instead of getChildIndex from ComparatorInterface
    private final double tau;
    private final Node<N_S, B> root;
    private final B statisticsBuilder;
    private long n = 0L;


    public HoeffdingTree(long classesNumber, double delta, int attributesNumber, double tau, long nMin, B statisticsBuilder) {
        this.R = (int) (Math.log(classesNumber) / Math.log(2));
        this.delta = delta;
        this.attributesNumber = attributesNumber;
        this.tau = tau;
        this.nMin = nMin;
        this.statisticsBuilder = statisticsBuilder;
        this.root = new Node<>(statisticsBuilder);
    }

    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example, ArrayList<Tuple2<String, Long>> performances) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        int predictedClass = leaf.getMajorityClass();
        logger.info(example + " predicted with " + predictedClass);
        return new Tuple2<>(predictedClass, performances);
    }

    @Override
    public String generateClassifierParams() {
        return "r" + R + "_d" + delta + "_t" + tau + "_n" + nMin;
    }

    @Override
    public void bootstrapTrainImplementation(Example example) {
        n++;
        Node<N_S, B> leaf = root;

        while (!leaf.isLeaf())
            leaf = leaf.getChild(example);

        updateLeaf(example, leaf);
    }

    private void updateLeaf(Example example, Node<N_S, B> leaf) {
        leaf.updateStatistics(example);

        if (leaf.getN() > nMin) {
            leaf.resetN();
            double eps = getEpsilon();

            HighestHeuristicPOJO pojo = new HighestHeuristicPOJO(leaf);

            if (pojo.attributeNumber == null) {
                String msg = "Hoeffding test showed no attributes";
                logger.info(msg);
                throw new RuntimeException(msg);
            } else if (pojo.hXa != null && pojo.hXb != null && (pojo.hXa - pojo.hXb > eps)) {
                logger.info("Heuristic value is correspondingly higher, splitting");
                leaf.split(pojo.attributeNumber, statisticsBuilder, example);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                leaf.split(pojo.attributeNumber, statisticsBuilder, example);
            } else logger.info("No split");
        } else logger.info("Not enough samples to test splits");
    }

    protected ArrayList<Tuple2<String, Long>> trainImplementation(Example example) throws RuntimeException {
        n++;
        ArrayList<Tuple2<String, Long>> trainingPerformances = new ArrayList<>();
        Node<N_S, B> leaf = getLeaf(example, trainingPerformances);
        logger.info("Training: " + example.toString());

        updateLeaf(example, leaf);

        logger.info(leaf.getStatistics().toString());

        return trainingPerformances;
    }

    private Node<N_S, B> getLeaf(Example example, ArrayList<Tuple2<String, Long>> performances) {
        Instant start = Instant.now();
        long count = 1;
        Node<N_S, B> result = root;
        while (!(result.isLeaf())) {
            count++;
            result = result.getChild(example);
        }
        performances.add(Tuple2.of(HoeffdingTreeFields.NODES_DURING_TRAVERSAL_COUNT, count));
        performances.add(Tuple2.of(HoeffdingTreeFields.DURING_TRAVERSAL_DURATION, Helpers.toNow(start)));
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
        final Integer attributeNumber;
        final Double hXa;
        final Double hXb;

        public HighestHeuristicPOJO(Node<N_S, B> node) {
            Double hXbTemp = null;
            Double hXaTemp = null;
            Integer xaTemp = null;
            Integer xbTemp = null;

            for (int i = 0; i < attributesNumber; i++) {
                double h = heuristic(i, node);
                if (xaTemp == null || h > hXaTemp) {
                    xaTemp = i;
                    hXaTemp = h;
                } else if (xbTemp == null || h > hXbTemp) {
                    xbTemp = i;
                    hXbTemp = h;
                }
            }

            hXb = hXbTemp;
            hXa = hXaTemp;
            attributeNumber = xaTemp;
        }
    }

    protected abstract double heuristic(int attributeIndex, Node<N_S, B> node);

//    public static void main(String[] args) {
//        String path = "/home/deikare/wut/streaming-datasets/" + "elec.csv";
//        HashSet<String> attributes = new HashSet<>();
//
//        try {
//            File file = new File(path);
//            Scanner scanner = new Scanner(file);
//
//            String line = scanner.nextLine();
//
//            String[] attributesAsString = line.split(",");
//            System.out.println(attributesAsString[attributesAsString.length - 1]);
//            int n = attributesAsString.length - 1;
//            attributes.addAll(Arrays.asList(attributesAsString).subList(0, n));
//
//            SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);
//            double delta = 0.05;
//            double tau = 0.2;
//            long nMin = 50;
//            long batchStatLength = 500;
//            long classesAmount = 2;
//
//
//            HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> tree = new HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>(classesAmount, delta, attributes, tau, nMin, statisticsBuilder) {
//                @Override
//                protected double heuristic(String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) {
//                    return 0;
//                }
//            };
//
//            while (scanner.hasNext()) {
//                line = scanner.nextLine();
//
//                String[] attributesValuesAsString = line.split(",");
//                HashMap<String, Double> attributesMap = new HashMap<>(n);
//                for (int i = 0; i < n; i++) {
//                    attributesMap.put(attributesAsString[i], Double.parseDouble(attributesValuesAsString[i]));
//                }
//
//                String className = attributesValuesAsString[n];
//                Example example = new Example(className, attributesMap);
//
//                tree.trainImplementation(example);
//
////                if (tree.classifyImplementation(example) == null)
////                    tree.classifyImplementation(example); //TODO spytać o to, czy najpierw powinna być predykcja, czy trening
//            }
//
////            System.out.println(tree.printStatistics());
////            tree.printStatisticsToFile(path);
//        } catch (FileNotFoundException e) {
//            System.out.println("No file found");
//            e.printStackTrace();
//        }
////        } catch (UnsupportedEncodingException e) {
////            throw new RuntimeException(e);
////        }
//    }


}
