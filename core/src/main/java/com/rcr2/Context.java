package com.rcr2;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

import java.util.*;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Context<F extends Frame<F>> {

    StateNodeTree stateTree = new StateNodeTree();

    StateNodeTree.StateNode find(SessionInput<F> sessionInput) {
        return stateTree.find(sessionInput.dependsOn());
    }

    static String TEXT = "text";

    @AllArgsConstructor
    @FieldDefaults(makeFinal = true)
    static class FunctionEntry<F extends Frame<F>> {
        @NonNull String name;
        int arity;
        @NonNull Function<F> function;
    }

    Map<String,FunctionEntry<F>> functions = new HashMap<>();

    public Context<F> withPureFunction(String name, int arity, Function.Pure<F> fn) {
        functions.put(name, new FunctionEntry<>(name, arity, fn));
        return this;
    }

    public Context<F> withSideEffect(String name, int arity, Function.SideEffect<F> fn) {
        functions.put(name, new FunctionEntry<F>(name, arity, fn));
        return this;
    }

    public Function<F> functionForName(String name) {
        val entry = functions.get(name);
        if (entry != null)
            return entry.function;
        return null;
    }

    public Integer arityForFunction(String name) {
        val entry = functions.get(name);
        if (entry != null)
            return entry.arity;
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
