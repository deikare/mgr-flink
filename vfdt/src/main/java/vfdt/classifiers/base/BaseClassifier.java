package vfdt.classifiers.base;

import vfdt.inputs.Example;

import java.io.Serializable;

public abstract class BaseClassifier implements Serializable {
    //todo optimize performance with not unpacking TupleN values - remove pieces of code TupleN<> x; x.f<whatever> - it wastes a lot of time
    public abstract String generateClassifierParams();

    public abstract void bootstrapTrainImplementation(Example example);
}
