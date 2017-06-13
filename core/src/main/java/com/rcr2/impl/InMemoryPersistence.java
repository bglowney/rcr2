package com.rcr2.impl;

import com.rcr2.Frame;
import com.rcr2.Persistence;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.util.*;

public class InMemoryPersistence<F extends Frame<F>> extends AbstractPersistence<F> {

    @RequiredArgsConstructor
    static class FeedbackStats implements Persistence.FeedbackStats {
        @Getter Integer count = 0;
        @Getter final String priorStatement;
        @Getter final String subsequentStatement;
        @Getter Integer cumulative = 0;

        @Override
        public void increment(int score) {
            if (count == null)
                count = 0;
            count++;
            if (cumulative == null)
                cumulative = 0;
            cumulative += score;
        }

        @Override
        public double getExpectedValue() {
            return (double)cumulative / (double)count;
        }

    }

    final Map<String,Map<String,Persistence.FeedbackStats>> data = new HashMap<>();

    @Override
    public Collection<? extends Persistence.FeedbackStats> getFeedbackStats(String currentState) {
        val nextMap = data.get(currentState);
        if (nextMap == null)
            return Collections.emptyList();

        return nextMap.values();
    }

    protected void addObservation(String previous, String next, int score) {
        data.computeIfAbsent(previous, p -> new HashMap<>())
                .computeIfAbsent(next, s -> new FeedbackStats(previous, next))
                .increment(score);
    }
}
