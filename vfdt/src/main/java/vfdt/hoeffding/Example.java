package vfdt.hoeffding;

import java.util.HashMap;

public class Example<K> {
    private K key;
    private String className;
    private HashMap<String, String> attributes;

    public Example() {
    }

    public Example(K key, String className, HashMap<String, String> attributes) {
        this.key = key;
        this.className = className;
        this.attributes = attributes;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(HashMap<String, String> attributes) {
        this.attributes = attributes;
    }
}
