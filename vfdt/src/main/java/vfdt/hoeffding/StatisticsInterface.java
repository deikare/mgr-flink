package vfdt.hoeffding;

public interface StatisticsInterface {
    void update(Example example);
    String getMajorityClass();

    double getSplittingValue(String attribute);
}
