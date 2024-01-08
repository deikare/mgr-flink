package vfdt.classifiers.base;

import java.time.Duration;
import java.time.Instant;

public abstract class BaseClassifier {
    public abstract String generateClassifierParams();

    protected long toNow(Instant start) {
        return Duration.between(start, Instant.now()).toNanos();
    }

    protected String timestampTrailingZeros(Instant start) {
        return start.getEpochSecond() + String.format("%09d", start.getNano());
    }
}
