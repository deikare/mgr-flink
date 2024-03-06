package vfdt.classifiers.bstHoeffding.standard;

import vfdt.classifiers.bstHoeffding.bstStatistics.BstHoeffdingTreeStatistics;
import vfdt.classifiers.hoeffding.HoeffdingTree;
import vfdt.classifiers.hoeffding.Node;
import vfdt.classifiers.hoeffding.NodeStatistics;
import vfdt.classifiers.hoeffding.StatisticsBuilderInterface;
import vfdt.inputs.Example;

public abstract class BstHoeffdingTree<N_S extends NodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends HoeffdingTree<N_S, B> {
    protected final BstHoeffdingTreeStatistics totalStatistics;

    public BstHoeffdingTree(int classesNumber, double delta, int attributesNumber, double tau, long nMin, B statisticsBuilder) {
        super(classesNumber, delta, attributesNumber, tau, nMin, statisticsBuilder);
        totalStatistics = new BstHoeffdingTreeStatistics(classesNumber, attributesNumber);
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
