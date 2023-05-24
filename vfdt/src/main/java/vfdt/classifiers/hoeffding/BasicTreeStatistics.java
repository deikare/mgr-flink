package vfdt.classifiers.hoeffding;

import java.text.DecimalFormat;

public class BasicTreeStatistics {
    private long n = 0;
    private long correctClassifications = 0;

    private static class TreeStat {
        private double nodesOnTraverseMeanCount = 0;
        private double toLeafTraverseMeanDuration = 0;
        private double meanTotalDuration = 0;

        public void update(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration, long n) {
            toLeafTraverseMeanDuration = (toLeafTraverseMeanDuration * n + (double) toLeafTraverseDuration) / (n + 1);
            nodesOnTraverseMeanCount = (nodesOnTraverseMeanCount * n + (double) nodesOnTraverseCount) / (n + 1);
            meanTotalDuration = (meanTotalDuration * n + (double) totalDuration) / (n + 1);
        }

        public TreeStat() {
        }

        @Override
        public String toString() {
            return "TreeStat{" +
                    "nodesOnTraverseMeanCount=" + nodesOnTraverseMeanCount +
                    ", toLeafTraverseMeanDuration=" + toLeafTraverseMeanDuration +
                    ", meanTotalDuration=" + meanTotalDuration +
                    '}';
        }
    }

    private final TreeStat classificationStats;
    private final TreeStat learningStats;

    public BasicTreeStatistics() {
        classificationStats = new TreeStat();
        learningStats = new TreeStat();
    }

    public void updateOnLearning(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration) {
        learningStats.update(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, n);
        n++;
    }

    public void updateOnClassification(long toLeafTraverseDuration, long nodesOnTraverseCount, long totalDuration, boolean isCorrect) {
        classificationStats.update(toLeafTraverseDuration, nodesOnTraverseCount, totalDuration, n);
        if (isCorrect)
            correctClassifications++;
    }

    public long getN() {
        return n;
    }

    @Override
    public String toString() {
        DecimalFormat format = new DecimalFormat("0.00");
        return "\t\tn=" + n +
                "\n\t\tcorrectClassifications = " + correctClassifications + " (accuracy: " + format.format((double) (100) * (double) (correctClassifications) / (double) (n)) + "%)" +
                "\n\t\t" +
                "\n\t\tclassificationStats = " + classificationStats +
                "\n\t\tlearningStats = " + learningStats;
    }

    public String toStringSimple() {
        return "Accuracy: " + ((double) (correctClassifications) / (double) (n)) + " (" + correctClassifications + "/" + n + ")";
    }
}
