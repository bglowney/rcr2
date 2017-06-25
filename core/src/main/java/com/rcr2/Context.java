package com.rcr2;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class Context<F extends Frame<F>, C extends Context<F,C>> {

    StateNodeTree stateTree = new StateNodeTree();

    StateNodeTree.StateNode find(SessionInput<F,C> sessionInput) {
        return stateTree.find(sessionInput.dependsOn());
    }

    SequenceProvider<F,C> sequenceProvider;

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true)
    static class FunctionEntry<F extends Frame<F>> {
        private @NonNull @Getter String name;
        int arity;
        private @NonNull @Getter Function<F> function;
    }

    Map<String,FunctionEntry<F>> functions = new HashMap<>();

    public void withPureFunction(String name, int arity, Function.Pure<F> fn) {
        functions.put(name, new FunctionEntry<>(name, arity, fn));
    }

    public void withSideEffect(String name, int arity, Function.SideEffect<F> fn) {
        functions.put(name, new FunctionEntry<>(name, arity, fn));
    }

    public boolean hasFunction(String name) {
        return functions.containsKey(name) || sequenceProvider.hasSequence(name);
    }

    public boolean isPureFunction(String name) {
        val entry = functions.get(name);
        if (entry != null)
            return entry.function instanceof Function.Pure;
        return hasFunction(name);
    }

    public Function<F> functionForName(String name, Session<F, C> session, F baseFrame) {
        val entry = functions.get(name);
        if (entry != null)
            return entry.function;
        Function.Sequence<F,C> sequence = sequenceProvider.forName(name, session, baseFrame);
        if (sequence != null)
            return sequence;

        return null;
    }

    public Integer arityForFunction(String name) {
        val entry = functions.get(name);
        if (entry != null)
            return entry.arity;
        if (sequenceProvider.hasSequence(name))
            return 0;
        return null;
    }

    public Collection<FunctionEntry<F>> allFunctions() {
        return functions.values();
    }

    public String displayFunctions() {
        return functions.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey()) // sort by function name alphabetically
                .map(entry -> entry.getKey() + "(" + entry.getValue().arity + ")")
                .collect(Collectors.joining("\n"));
    }

}
