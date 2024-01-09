package vfdt.inputs;

import java.util.Arrays;

public class Example {
    private int mappedClass;
    private double[] attributes;

    public Example() {
    }

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

    public void setMappedClass(int mappedClass) {
        this.mappedClass = mappedClass;
    }

    public void setAttributes(double[] attributes) {
        this.attributes = attributes;
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
