package vfdt.classifiers.hoeffding;

public class SimpleNodeStatisticsBuilder implements StatisticsBuilderInterface<SimpleNodeStatistics> {
    private final int attributesNumber;

    public SimpleNodeStatisticsBuilder(int attributesNumber) {
        this.attributesNumber = attributesNumber;
    }

    @Override
    public SimpleNodeStatistics build() {
        return new SimpleNodeStatistics(this.attributesNumber);
    }
}
