package vfdt.hoeffding;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class TreeStatistics {
    private TreeStatCombined totalStats;
    private long batchLength;
    private LinkedList<Long> samplesOnSplit;

    private LinkedList<TreeStatCombined> batchStats;

    public TreeStatistics(long batchLength) {
        this.batchLength = batchLength;

        totalStats = new TreeStatCombined();
        batchStats = new LinkedList<>();
        samplesOnSplit = new LinkedList<>();
        batchStats.addLast(new TreeStatCombined());
    }

    public void updateOnLearning(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration) {
        totalStats.updateOnLearning(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration);
        batchStats.getLast().updateOnLearning(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration);
        if (totalStats.getN() % batchLength == 0)
            batchStats.addLast(new TreeStatCombined());
    }

    public void updateOnClassification(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration, boolean isCorrect) {
        totalStats.updateOnClassification(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, isCorrect);
        batchStats.getLast().updateOnClassification(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, isCorrect);
    }

    public void updateOnNodeSplit() {
        samplesOnSplit.addLast(totalStats.getN());
    }

    @Override
    public String toString() {
        return "TreeStatistics:" +
                "\ntotalStats:\n" + totalStats +
                "\nbatchLength:\n" + batchLength +
                "\nsamplesOnSplit: " + samplesOnSplit +
                "\nbatchStats:\n" + batchStats.stream().map(TreeStatCombined::toString).collect(Collectors.joining("\n")) +
                '}';
    }

    public String totalStatisticsToString() {
        return "totalStats:\n" + totalStats;
    }
}
