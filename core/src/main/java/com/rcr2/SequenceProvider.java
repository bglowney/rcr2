package com.rcr2;

public interface SequenceProvider<F extends Frame<F>, C extends Context<F,C>> {

    boolean hasSequence(String name);

    Function.Sequence<F,C> forName(String name, Session<F,C> session, F baseFrame);
}
