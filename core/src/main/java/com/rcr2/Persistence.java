package com.rcr2;

public interface Persistence<F extends Frame<F>> {

    String bestFor(String currentState, int minObservations);

    void update(WorkingMemory<F> workingMemory, int score);

    void update(WorkingMemory<F> workingMemory, SessionInput<F> sideEffectInput, int score);

    interface FeedbackStats extends Comparable<FeedbackStats> {
        Integer getCount();
        String getPriorStatement();
        String getSubsequentStatement();
        double getExpectedValue();
        void increment(int value);

        default int compareTo(Persistence.FeedbackStats o) {
            return -1 * Double.compare(this.getExpectedValue(), o.getExpectedValue());
        }
    }
}
