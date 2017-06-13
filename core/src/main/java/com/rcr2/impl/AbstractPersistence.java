package com.rcr2.impl;

import com.rcr2.Frame;
import com.rcr2.Persistence;
import com.rcr2.SessionInput;
import com.rcr2.WorkingMemory;
import lombok.val;

import java.util.Collection;
import java.util.Comparator;

public abstract class AbstractPersistence<F extends Frame<F>> implements Persistence<F> {

    public abstract Collection<? extends FeedbackStats> getFeedbackStats(String currentState);

    @Override
    public String bestFor(String currentState, int minObservations) {
        return getFeedbackStats(currentState)
                .stream()
                .filter(entry -> entry.getCount() >= minObservations)
                .sorted(Comparator.naturalOrder())
                .findFirst()
                .map(FeedbackStats::getSubsequentStatement)
                .map(script -> {
                    return script.startsWith(Frame.EMPTY_ANNOTATION) ||
                            script.startsWith(Frame.FAILED_ANNOTATION)
                            ? script.substring(1)
                            : script;
                })
                .orElse(null);
    }

    @Override
    public void update(WorkingMemory<F> workingMemory, int score) {
        update(workingMemory, null, score);
    }

    @Override
    public void update(WorkingMemory<F> workingMemory, SessionInput<F> sideEffectInput, int score) {
        String previousDependencies = WorkingMemory.DEFAULT_STATE_SERIALIZATION;
        for (val alias : workingMemory.getEntriesByAlias().keySet()) {
            // only update the most recent entries
            if (!workingMemory.inScope(alias))
                continue;

            val entry = workingMemory.getEntriesByAlias().get(alias);
            // candidate is the script for the next best action
            val candidate = entry.getSessionInput().serializeStatement();
            addObservation(previousDependencies, candidate, score);
            for (val failure: entry.getFailures())
                addObservation(previousDependencies, failure.getFailedInput().serializeStatement(), failure.getFeedback());

            previousDependencies = workingMemory.serializePrevious(alias);

            // if this is the last step add the sideEffectInput's score, too
            if (sideEffectInput != null && entry.getStep() == workingMemory.getCurrentStep() - 1) {
                addObservation(previousDependencies,
                        sideEffectInput.serializeStatement(),
                        score);
            }
        }
    }

    protected abstract void addObservation(String previous, String next, int score);
}