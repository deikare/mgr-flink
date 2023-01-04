package vfdt.hoeffding;
public interface ComparatorInterface<K> {
    int getChild(Example<K> example); //method to get index of child aka compare

}
