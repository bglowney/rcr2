package com.rcr2.impl;

import com.rcr2.*;
import lombok.val;

import java.util.Collection;
import java.util.Comparator;

public abstract class AbstractPersistence<F extends Frame<F>, C extends Context<F,C>> implements Persistence<F,C> {

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
    public void update(Session<F,C> sessoin, int score) {
        update(sessoin, null, score);
    }

    @Override
    public void update(Session<F,C> session, SessionInput<F,C> sideEffectInput, int score) {
        String previousDependencies = Session.DEFAULT_STATE_SERIALIZATION;
        for (val alias : session.getEntriesByAlias().keySet()) {
            // only update the most recent entries
            if (!session.inScope(alias)) continue;

            val entry = session.getEntriesByAlias().get(alias);

            // if is substep in a sequence we don't record feedback
            if (entry.isInSequence()) continue;

            // candidate is the script for the next best action
            val candidate = entry.getSessionInput().serializeStatement();
            addObservation(previousDependencies, candidate, score);
            for (val failure: entry.getFailures())
                addObservation(previousDependencies, failure.getFailedInput().serializeStatement(), failure.getFeedback());

            previousDependencies = session.serializePrevious(alias);

            // if this is the last step add the sideEffectInput's score, too
            if (sideEffectInput != null && entry.getStep() == session.getCurrentStep() - 1) {
                addObservation(previousDependencies,
                        sideEffectInput.serializeStatement(),
                        score);
            }
        }
    }

    protected abstract void addObservation(String previous, String next, int score);
}