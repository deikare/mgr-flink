package vfdt.classifiers.bstHoeffding.functional;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.bstHoeffding.standard.BstHoeffdingTree;
import vfdt.classifiers.hoeffding.Node;
import vfdt.classifiers.hoeffding.StatisticsBuilderInterface;
import vfdt.inputs.Example;

import java.util.ArrayList;

public abstract class FunctionalBstHoeffdingTree<N_S extends NaiveBayesNodeStatistics, B extends StatisticsBuilderInterface<N_S>> extends BstHoeffdingTree<N_S, B> {
    public FunctionalBstHoeffdingTree(int classesNumber, double delta, int attributesNumber, double tau, long nMin, B statisticsBuilder) {
        super(classesNumber, delta, attributesNumber, tau, nMin, statisticsBuilder);
    }

    @Override
    protected Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classifyImplementation(Example example, ArrayList<Tuple2<String, Long>> performances) throws RuntimeException {
        Node<N_S, B> leaf = getLeaf(example);
        int predictedClass = leaf.getStatistics().getMajorityClass(example, totalStatistics.getAttributeCountsTrees());
        logger.info(example + " predicted with " + predictedClass);
        return new Tuple2<>(predictedClass, performances);
    }
}
