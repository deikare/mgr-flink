package vfdt.hoeffding;

import java.util.LinkedList;
import java.util.stream.Collectors;

public class AllTreeStatistics {
    private TreeTotalStatistics totalStats;
    private long batchLength;
    private LinkedList<Long> samplesOnSplit;

    private LinkedList<BasicTreeStatistics> batchStats;

    public AllTreeStatistics(long batchLength) {
        this.batchLength = batchLength;

        totalStats = new TreeTotalStatistics();
        batchStats = new LinkedList<>();
        samplesOnSplit = new LinkedList<>();
        batchStats.addLast(new BasicTreeStatistics());

    }

    public void updateOnLearning(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration) {
        totalStats.updateOnLearning(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration);
        batchStats.getLast().updateOnLearning(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration);
        if (totalStats.getN() % batchLength == 0)
            batchStats.addLast(new BasicTreeStatistics());
    }

    public void updateOnClassification(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration, boolean isCorrect) {
        totalStats.updateOnClassification(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, isCorrect);
        batchStats.getLast().updateOnClassification(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, isCorrect);
    }

    public void updateOnNodeSplit(boolean isReasonTau) {
        samplesOnSplit.addLast(totalStats.getN());
        if (isReasonTau)
            totalStats.incTauSplitsCount();
        else totalStats.incHeuristicSplitsCount();
    }

    @Override
    public String toString() {
        return "TreeStatistics:" +
                "\ntotalStats:\n" + totalStats +
                "\nbatchLength:\n" + batchLength +
                "\nsamplesOnSplit: " + samplesOnSplit +
                "\nbatchStats:\n" + batchStats.stream().map(BasicTreeStatistics::toString).collect(Collectors.joining("\n")) +
                '}';
    }

    public String totalStatisticsToString() {
        return "totalStats:\n" + totalStats;
    }
}