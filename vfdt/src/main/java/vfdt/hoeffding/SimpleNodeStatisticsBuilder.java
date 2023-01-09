package vfdt.hoeffding;

import java.util.HashSet;

public class SimpleNodeStatisticsBuilder implements StatisticsBuilderInterface<NodeWithAttributeValueCountsStatistics> {
    private HashSet<String> classNames;
    private HashSet<String> attributes;

    public SimpleNodeStatisticsBuilder(HashSet<String> classNames, HashSet<String> attributes) {
        this.classNames = classNames;
        this.attributes = attributes;
    }

    @Override
    public NodeWithAttributeValueCountsStatistics build() {
        return new NodeWithAttributeValueCountsStatistics(this.classNames, this.attributes);
    }
}
