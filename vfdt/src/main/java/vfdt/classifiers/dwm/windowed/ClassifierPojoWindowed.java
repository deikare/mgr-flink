package vfdt.classifiers.dwm.windowed;

import org.apache.flink.api.java.tuple.Tuple2;
import vfdt.classifiers.base.ClassifierPojo;
import vfdt.classifiers.dwm.classic.ClassifierInterface;
import vfdt.inputs.Example;

import java.util.ArrayList;
import java.util.LinkedList;

public class ClassifierPojoWindowed<C extends ClassifierInterface> extends ClassifierPojo<C> {
    private int wrongClassificationsCount;
    private final LinkedList<Integer> classificationsWindow;
    private final int windowSize;

    public ClassifierPojoWindowed(C classifier, long sampleNumber, int windowSize) {
        super(classifier, sampleNumber);
        this.windowSize = windowSize;
        wrongClassificationsCount = 0;
        classificationsWindow = new LinkedList<>();
    }

    public int getWrongClassificationsCount() {
        return wrongClassificationsCount;
    }

    @Override
    public Tuple2<Integer, ArrayList<Tuple2<String, Long>>> classify(Example example) {
        Tuple2<Integer, ArrayList<Tuple2<String, Long>>> result = super.classify(example);

        if (example.getMappedClass() == result.f0)
            classificationsWindow.add(0);
        else {
            wrongClassificationsCount++;
            classificationsWindow.add(1);
        }

        if (classificationsWindow.size() > windowSize) {
            int removedIncrement = classificationsWindow.remove();
            wrongClassificationsCount -= removedIncrement;
        }

        return result;
    }
}
