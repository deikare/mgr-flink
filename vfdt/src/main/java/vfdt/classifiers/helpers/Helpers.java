package vfdt.classifiers.helpers;

public class Helpers {
    public static <A extends Comparable<A>> int getIndexOfHighestValue(A[] tab) {
        int resultIndex = 0;
        A max = tab[0];

        for (int i = 1; i < tab.length; i++) {
            A comparedValue = tab[i];
            if (comparedValue.compareTo(max) > 0) {
                resultIndex = i;
                max = comparedValue;
            }
        }

        return resultIndex;
    }
}
