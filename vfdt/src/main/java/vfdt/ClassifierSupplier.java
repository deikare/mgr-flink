package vfdt;

import vfdt.hoeffding.BaseClassifier;

import java.io.Serializable;
import java.util.function.Supplier;

public interface ClassifierSupplier<C extends BaseClassifier> extends Supplier<C>, Serializable {
}
