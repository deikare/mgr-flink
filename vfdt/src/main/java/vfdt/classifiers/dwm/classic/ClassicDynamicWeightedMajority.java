package vfdt.classifiers.dwm.classic;

import vfdt.classifiers.base.BaseDynamicWeightedMajority;
import vfdt.classifiers.base.ClassifierPojo;

public abstract class ClassicDynamicWeightedMajority<C extends ClassifierInterface> extends BaseDynamicWeightedMajority<C, ClassifierPojo<C>> {

    protected ClassicDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        super(beta, threshold, classNumber, updateClassifiersEachSamples);
    }

    @Override
    protected boolean shouldNormalizeWeightsAndDeleteClassifiers() {
        return sampleNumber % updateClassifiersEachSamples == 0;
    }

    @Override
    protected void normalizeWeightsAndDeleteClassifiersSideEffects() {

    }

    @Override
    protected long lowerWeightAndReturnWeightLoweringCount(ClassifierPojo<C> classifierPojo, long weightsLoweringCount) {
        if (sampleNumber % updateClassifiersEachSamples == 0) {
            weightsLoweringCount++;
            classifierPojo.lowerWeight(beta);
        }
        return weightsLoweringCount;
    }

    @Override
    protected ClassifierPojo<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojo<>(createClassifier(), sampleNumber);
    }
}
