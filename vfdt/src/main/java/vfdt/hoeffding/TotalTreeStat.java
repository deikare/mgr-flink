package vfdt.hoeffding;

public class TotalTreeStat extends TreeBasicStat {
    private long heuristicSplitsCount;
    private long tauSplitsCount;

    public TotalTreeStat() {
        super();
        heuristicSplitsCount = 0;
        tauSplitsCount = 0;
    }

    public void incHeuristicSplitsCount() {
        heuristicSplitsCount++;
    }

    public void incTauSplitsCount() {
        tauSplitsCount++;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\n\t\theuristicSplitsCount = " + heuristicSplitsCount +
                "\n\t\ttauSplitsCount = " + tauSplitsCount;
    }
}