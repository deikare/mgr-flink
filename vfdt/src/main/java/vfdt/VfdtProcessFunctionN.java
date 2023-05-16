package vfdt;

import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.configuration.Configuration;
import vfdt.hoeffding.HoeffdingTree;
import vfdt.hoeffding.SimpleNodeStatistics;
import vfdt.hoeffding.SimpleNodeStatisticsBuilder;

import java.util.function.Supplier;

public class VfdtProcessFunctionN extends BaseProcessFunction<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> {
    public VfdtProcessFunctionN(ClassifierSupplier<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> createClassifierFunction) {
        super(createClassifierFunction);
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        TypeInformation<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>> classifierInfo = TypeInformation.of(new TypeHint<HoeffdingTree<SimpleNodeStatistics, SimpleNodeStatisticsBuilder>>() { //todo exception here - try to put this line as lambda in main
        });
        classifierState = getRuntimeContext().getState(new ValueStateDescriptor<>("classifier", classifierInfo));
//        if (classifierState.value() == null)
//            classifierState.update(createClassifierFunction.get());
    }
}
