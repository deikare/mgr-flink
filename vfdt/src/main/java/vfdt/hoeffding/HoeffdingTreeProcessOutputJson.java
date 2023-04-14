package vfdt.hoeffding;

public class HoeffdingTreeProcessOutputJson extends ProcessOutputBaseJson {
    private final long nodesDuringTraversalCount;
    private final long duringTraversalDuration;

    public HoeffdingTreeProcessOutputJson(long timestamp, String classifierType, String classifierParams, String experimentId, String dataset, long learningDuration, long classificationDuration, long samplesTotal, String classLabel, String predictedLabel, long nodesDuringTraversalCount, long duringTraversalDuration) {
        super(timestamp, classifierType, classifierParams, experimentId, dataset, learningDuration, classificationDuration, samplesTotal, classLabel, predictedLabel);
        this.nodesDuringTraversalCount = nodesDuringTraversalCount;
        this.duringTraversalDuration = duringTraversalDuration;
    }

    public long getNodesDuringTraversalCount() {
        return nodesDuringTraversalCount;
    }

    public long getDuringTraversalDuration() {
        return duringTraversalDuration;
    }
}
