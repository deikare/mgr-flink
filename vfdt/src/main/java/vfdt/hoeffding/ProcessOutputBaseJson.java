package vfdt.hoeffding;

public class ProcessOutputBaseJson {
    private final long timestamp;
    private final String tag_classifierType; //marking of tags for Influxdb
    private final String tag_classifierParams;
    private final String tag_experimentId;
    private final String tag_dataset;
    private final long learningDuration;
    private final long classificationDuration;
    private final long samplesTotal;
    private final String tag_classLabel;
    private final String tag_predictedLabel; //so correctClassifications is

    public ProcessOutputBaseJson(long timestamp, String classifierType, String classifierParams, String experimentId, String dataset, long learningDuration, long classificationDuration, long samplesTotal, String classLabel, String predictedLabel) {
        this.timestamp = timestamp;
        this.tag_classifierType = classifierType;
        this.tag_classifierParams = classifierParams;
        this.tag_experimentId = experimentId;
        this.tag_dataset = dataset;
        this.learningDuration = learningDuration;
        this.classificationDuration = classificationDuration;
        this.samplesTotal = samplesTotal;
        this.tag_classLabel = classLabel;
        this.tag_predictedLabel = predictedLabel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTag_classifierType() {
        return tag_classifierType;
    }

    public String getTag_classifierParams() {
        return tag_classifierParams;
    }

    public String getTag_dataset() {
        return tag_dataset;
    }

    public long getLearningDuration() {
        return learningDuration;
    }

    public long getClassificationDuration() {
        return classificationDuration;
    }

    public long getSamplesTotal() {
        return samplesTotal;
    }

    public String getTag_classLabel() {
        return tag_classLabel;
    }

    public String getTag_predictedLabel() {
        return tag_predictedLabel;
    }
}
