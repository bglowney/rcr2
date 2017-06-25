package com.rcr2;

import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface Function<F extends Frame<F>> {

    Optional<F> apply(List<F> args);

    interface Pure<F extends Frame<F>> extends Function<F> {}
    interface SideEffect<F extends Frame<F>> extends Function<F> {}

    /**
     * An sequence is a wrapper for multiple pure statements
     */
    @RequiredArgsConstructor
    class Sequence<F extends Frame<F>, C extends Context<F,C>> implements Pure<F> {
        final Session<F,C> session;
        final F baseFrame;
        final List<String> scripts;
        final List<String> components;

        @Override
        public Optional<F> apply(List<F> args) {
            return session.inSequenceStep(this);
        }
    }

}
