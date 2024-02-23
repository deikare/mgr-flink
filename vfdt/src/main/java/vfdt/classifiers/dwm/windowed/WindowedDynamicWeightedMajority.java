package vfdt.classifiers.dwm.windowed;

import vfdt.classifiers.base.BaseDynamicWeightedMajority;
import vfdt.classifiers.dwm.classic.ClassifierInterface;

public abstract class WindowedDynamicWeightedMajority<C extends ClassifierInterface> extends BaseDynamicWeightedMajority<C, ClassifierPojoWindowed<C>> {
    private final int windowSize;
    private boolean anyWeightChanged;

    public WindowedDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples, int windowSize) {
        super(beta, threshold, classNumber, updateClassifiersEachSamples);
        this.windowSize = windowSize;
        anyWeightChanged = false;
    }

    @Override
    public String generateClassifierParams() {
        return super.generateClassifierParams() + "_w" + windowSize;
    }

    @Override
    protected ClassifierPojoWindowed<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojoWindowed<>(createClassifier(), sampleNumber, windowSize);
    }

    @Override
    protected boolean shouldNormalizeWeightsAndDeleteClassifiers() {
        return anyWeightChanged;
    }

    @Override
    protected void normalizeWeightsAndDeleteClassifiersSideEffects() {
        anyWeightChanged = false;
    }

    @Override
    protected long lowerWeightAndReturnWeightLoweringCount(ClassifierPojoWindowed<C> classifierPojo, long weightsLoweringCount) {
        if (classifierPojo.getWrongClassificationsCount() > updateClassifiersEachSamples) {
            classifierPojo.lowerWeight(beta);
            anyWeightChanged = true;
            weightsLoweringCount++;
        }

        return weightsLoweringCount;
    }
}
