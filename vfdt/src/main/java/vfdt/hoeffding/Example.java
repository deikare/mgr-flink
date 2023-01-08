package vfdt.hoeffding;

import java.util.HashMap;

public class Example {
    private String className;
    private HashMap<String, Double> attributes;

    public Example() {
    }

    public Example( String className, HashMap<String, Double> attributes) {
        this.className = className;
        this.attributes = attributes;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public HashMap<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, Double> attributes) {
        this.attributes = attributes;
    }
}
