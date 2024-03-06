package vfdt.classifiers.bstHoeffding;

import vfdt.classifiers.hoeffding.StatisticsBuilderInterface;

public class NaiveBayesNodeStatisticsBuilder implements StatisticsBuilderInterface<NaiveBayesNodeStatistics> {
    private final int classNumber;
    private final int attributesNumber;
    private final int maxDifferentValuesCount;

    public NaiveBayesNodeStatisticsBuilder(int classNumber, int attributesNumber, int maxDifferentValuesCount) {
        this.classNumber = classNumber;
        this.attributesNumber = attributesNumber;
        this.maxDifferentValuesCount = maxDifferentValuesCount;
    }

    @Override
    public NaiveBayesNodeStatistics build() {
        return new NaiveBayesNodeStatistics(classNumber, attributesNumber, maxDifferentValuesCount);
    }
}
