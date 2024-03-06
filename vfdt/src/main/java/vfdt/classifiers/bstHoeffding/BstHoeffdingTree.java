package vfdt.classifiers.bstHoeffding;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.bstHoeffding.bstStatistics.BstHoeffdingTreeStatistics;
import vfdt.classifiers.hoeffding.HoeffdingTree;
import vfdt.classifiers.hoeffding.Node;
import vfdt.classifiers.hoeffding.StatisticsBuilderInterface;
import vfdt.inputs.Example;

import java.util.ArrayList;

public abstract class BstHoeffdingTree<N_S extends NaiveBayesNodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends HoeffdingTree<N_S, B> {
    private final BstHoeffdingTreeStatistics totalStatistics;

    public BstHoeffdingTree(int classesNumber, double delta, int attributesNumber, double tau, long nMin, B statisticsBuilder) {
        super(classesNumber, delta, attributesNumber, tau, nMin, statisticsBuilder);
        totalStatistics = new BstHoeffdingTreeStatistics(classesNumber, attributesNumber);
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example, ArrayList<Tuple2<String, Long>> performances) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        int predictedClass = leaf.getStatistics().getMajorityClass(example, totalStatistics.getAttributeCountsTrees());
        logger.info(example + " predicted with " + predictedClass);
        return new Tuple2<>(predictedClass, performances);
    }

    @Override
    protected void updateLeaf(Example example, Node<N_S, B> leaf) {
        leaf.updateStatistics(example);
        totalStatistics.updateStatistics(example);

        if (leaf.getN() > nMin) {
            leaf.resetN();
            double eps = getEpsilon();

            HighestHeuristicPOJO pojo = new HighestHeuristicPOJO(leaf);

            if (pojo.xA == null) {
                String msg = "Hoeffding test showed no attributes";
                logger.info(msg);
                throw new RuntimeException(msg);
            } else if (pojo.hXa != null && pojo.hXb != null && (pojo.hXa - pojo.hXb > eps)) {
                logger.info("Heuristic value is correspondingly higher, splitting");
                findSplittingValueAndSplit(example, leaf, pojo.xA);
            } else if (eps < tau) {
                logger.info("Epsilon is lower than tau, splitting");
                findSplittingValueAndSplit(example, leaf, pojo.xA);
            } else logger.info("No split");
        } else logger.info("Not enough samples to test splits");
    }

    private void findSplittingValueAndSplit(Example example, Node<N_S, B> leaf, int splittingAttributeIndex) {
        double splittingValue = totalStatistics.getSplittingValue(splittingAttributeIndex, n);
        leaf.split(splittingAttributeIndex, splittingValue, statisticsBuilder, example);
    }
}
