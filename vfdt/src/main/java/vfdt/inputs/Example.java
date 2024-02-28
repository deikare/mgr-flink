package vfdt.inputs;

import java.util.Arrays;

public class Example {
    private final int mappedClass;
    private final double[] attributes;

    public Example(int mappedClass, double[] attributes) {
        this.mappedClass = mappedClass;
        this.attributes = attributes;
    }

    public int getMappedClass() {
        return mappedClass;
    }

    public double[] getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "Example{" +
                "mappedClass='" + mappedClass + '\'' +
                ", attributes=" + Arrays.toString(attributes) +
                '}';
    }

    public long getId() {
        return 0L;
    }
}
