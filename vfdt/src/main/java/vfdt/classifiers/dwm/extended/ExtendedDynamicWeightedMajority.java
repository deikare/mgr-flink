package vfdt.classifiers.dwm.extended;

import vfdt.classifiers.base.BaseDynamicWeightedMajority;
import vfdt.classifiers.dwm.classic.ClassifierInterface;

public abstract class ExtendedDynamicWeightedMajority<C extends ClassifierInterface> extends BaseDynamicWeightedMajority<C, ClassifierPojoExtended<C>> {
    protected boolean anyWeightChanged; //two ideas - first to use flag, second to use array of lowered weight classifiers indices - second idea doesn't work because still all classifiers should be normalized

    public ExtendedDynamicWeightedMajority(double beta, double threshold, int classNumber, int updateClassifiersEachSamples) {
        super(beta, threshold, classNumber, updateClassifiersEachSamples);
        anyWeightChanged = false;
    }

    @Override
    protected ClassifierPojoExtended<C> createClassifierWithWeight(long sampleNumber) {
        return new ClassifierPojoExtended<>(createClassifier(), sampleNumber);
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
    protected long lowerWeightAndReturnWeightLoweringCount(ClassifierPojoExtended<C> classifierPojo, long weightsLoweringCount) {
        if (classifierPojo.getWrongClassificationsCounter() % updateClassifiersEachSamples == 0) {
            classifierPojo.clearWrongClassificationCounter();
            classifierPojo.lowerWeight(beta);
            //prawdopodobnie tutaj jest błąd, który poprawia skuteczność - nie wiem, czy rzeczywiście na zewnątrz funkcji classifierPojo jest modyfikowany
            anyWeightChanged = true;
            weightsLoweringCount++;
        }
        return weightsLoweringCount;
    }
}
