package vfdt.hoeffding;

import java.io.Serializable;

public interface StatisticsInterface extends Serializable {
    void update(Example example);

    String getMajorityClass();

    double getSplittingValue(String attribute);
}
