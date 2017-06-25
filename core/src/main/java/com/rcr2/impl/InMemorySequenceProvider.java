package com.rcr2.impl;

import com.rcr2.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemorySequenceProvider<F extends Frame<F>, C extends Context<F,C>> implements SequenceProvider<F,C> {

    @RequiredArgsConstructor
    public static class Sequence {
        @Getter
        final String name;
        @Getter @Setter
        final List<String> scripts;
        @Getter @Setter
        final List<String> components;
    }

    final Map<String,Sequence> sequences = new HashMap<>();

    public InMemorySequenceProvider addSequence(Sequence sequence) {
        sequences.put(sequence.name, sequence);
        return this;
    }

    @Override
    public boolean hasSequence(String name) {
        return sequences.containsKey(name);
    }

    @Override
    public Function.Sequence<F,C> forName(String name, Session<F,C> session, F baseFrame) {
        val sequence = sequences.get(name);
        if (sequence == null)
            return null;
        return new Function.Sequence<>(session, baseFrame, sequence.getScripts(), sequence.getComponents());
    }
}
