package vfdt.classifiers.base;

import vfdt.inputs.Example;

import java.io.Serializable;

public abstract class BaseClassifier implements Serializable {
    public abstract String generateClassifierParams();

    public abstract void bootstrapTrainImplementation(Example example);
}
