package vfdt.processors.coding;

import java.util.HashMap;

public class Encoder {
    HashMap<String, Integer> mappings = new HashMap<>();
    private int newMapping = 0;

    public int encode(String className) {
        Integer result = mappings.putIfAbsent(className, newMapping);
        if (result == null)
            result = newMapping;
        newMapping++;
        return result;
    }

    public HashMap<Integer, String> decoder() {
        HashMap<Integer, String> result = new HashMap<>();
        mappings.forEach((className, mapping) -> result.put(mapping, className));
        return result;
    }
}
