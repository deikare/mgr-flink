package vfdt.classifiers.hoeffding;

import java.util.HashSet;

public class SimpleNodeStatisticsBuilder implements StatisticsBuilderInterface<SimpleNodeStatistics> {
    private final HashSet<String> attributes;

    public SimpleNodeStatisticsBuilder(HashSet<String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SimpleNodeStatistics build() {
        return new SimpleNodeStatistics(this.attributes);
    }
}
