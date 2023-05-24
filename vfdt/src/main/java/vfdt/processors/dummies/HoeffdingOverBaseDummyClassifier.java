package vfdt.processors.dummies;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import vfdt.classifiers.hoeffding.HoeffdingTree;
import vfdt.classifiers.hoeffding.Node;
import vfdt.classifiers.hoeffding.SimpleNodeStatistics;
import vfdt.classifiers.hoeffding.SimpleNodeStatisticsBuilder;

import java.util.Arrays;
import java.util.HashSet;

public abstract class HoeffdingOverBaseDummyClassifier extends BaseDummyProcessFunction<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> {
    public HoeffdingOverBaseDummyClassifier(String dataset) {
        super("vfdt", dataset);
    }

//    @Override
//    public void open(Configuration parameters) throws Exception {
//        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> typeInformation = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
//        });
//        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("hoeffding", typeInformation));
//    }

    @Override
    protected void registerClassifier() {
        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> typeInformation = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() {
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("hoeffding", typeInformation));
    }

    //    @Override
//    protected HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> createClassifier() {
//        double delta = 0.05;
//        double tau = 0.2;
//        long nMin = 50;
//        long batchStatLength = 500;
//        long classesAmount = 2;
//        HashSet<String> attributes = new HashSet<>(Arrays.asList("period,nswprice,nswdemand,vicprice,vicdemand,transfer".split(",")));
//
//        SimpleNodeStatisticsBuilder statisticsBuilder = new SimpleNodeStatisticsBuilder(attributes);
//        return new HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>(classesAmount, delta, attributes, tau, nMin, statisticsBuilder, batchStatLength) {
//            @Override
//            protected double heuristic(String attribute, Node<SimpleNodeStatistics, SimpleNodeStatisticsBuilder> node) {
//                double threshold = 0.5;
//                return Math.abs(threshold - node.getStatistics().getSplittingValue(attribute)) / threshold;
//            }
//        };
//    }
}
