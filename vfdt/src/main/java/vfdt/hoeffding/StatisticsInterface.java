package vfdt.hoeffding;

public interface StatisticsInterface<K> {
    void update(Example<K> example);
    String getMajorityClass();
}
