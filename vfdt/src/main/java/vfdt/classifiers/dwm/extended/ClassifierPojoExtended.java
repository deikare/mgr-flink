package vfdt.classifiers.dwm.extended;

import vfdt.classifiers.base.ClassifierPojo;
import vfdt.classifiers.dwm.classic.ClassifierInterface;

public class ClassifierPojoExtended<C extends ClassifierInterface> extends ClassifierPojo<C> {
    protected long wrongClassificationsCounter;

    public ClassifierPojoExtended(C classifier, long sampleNumber) {
        super(classifier, sampleNumber);
        clearWrongClassificationCounter();
    }

    public long incWrongClassificationCounter() {
        wrongClassificationsCounter++;
        return wrongClassificationsCounter;
    }

    public void clearWrongClassificationCounter() {
        wrongClassificationsCounter = 0L;
    }
}
