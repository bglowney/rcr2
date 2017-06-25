package com.rcr2;

public interface Persistence<F extends Frame<F>, C extends Context<F,C>> {

    String bestFor(String currentState, int minObservations);

    void update(Session<F,C> workingMemory, int score);

    void update(Session<F,C> workingMemory, SessionInput<F,C> sideEffectInput, int score);

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
