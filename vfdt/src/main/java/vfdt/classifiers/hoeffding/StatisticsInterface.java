package vfdt.classifiers.hoeffding;

import vfdt.inputs.Example;

import java.io.Serializable;

public interface StatisticsInterface extends Serializable {
    void update(Example example);

    int getMajorityClass();

    double getSplittingValue(int attributeNumber);
}
