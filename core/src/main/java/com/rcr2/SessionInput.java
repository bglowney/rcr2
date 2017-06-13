package com.rcr2;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SessionInput<F extends Frame<F>> {

    @Getter @Setter
    private String function;
    @Getter @Setter
    protected List<String> args = new ArrayList<>();
    @Setter
    protected String alias;
    protected Optional<F> result;

    protected final Context<F> context;
    protected final WorkingMemory<F> workingMemory;

    private String serialization;

    public SessionInput(@NotNull Context<F> context, @NotNull WorkingMemory<F> workingMemory) {
        this.context = context;
        this.workingMemory = workingMemory;
    }

    public void init() {
        this.serialization = this.serializeStatement();
    }

    public void addArg(String arg) {
        args.add(arg);
    }

    public boolean hasSideEffect(Context<F> context) {
        return context.functionForName(function) instanceof Function.SideEffect;
    }

    /**
     * Get the args of this sessionInput in serialized form,
     * and concatenate with the args of all children
     */
    public SortedSet<String> dependsOn() {
        return Stream.concat(args.stream().map(arg -> workingMemory.getInput(arg).serializeStatement()),
                      args.stream().flatMap(arg -> workingMemory.getInput(arg).dependsOn().stream()))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<F> apply() {
        if (this.result == null)
            this.result = context.functionForName(function).apply(getArgFrames(workingMemory));
        return this.result;
    }

    public Optional<String> getAlias() {
        if (alias != null)
            return Optional.of(alias);
        return Optional.empty();
    }

    public String serializeStatement() {
        if (this.serialization != null)
            return this.serialization;

        if (this.result == null)
            this.result = this.apply();

        final String annotation = result
                .map(f -> f instanceof EmptyFrame ? Frame.EMPTY_ANNOTATION : "")
                .orElse(Frame.FAILED_ANNOTATION);

        return annotation + this.function + " (" +
            args.stream()
            .map(arg -> workingMemory.getInput(arg))
            .map(arg -> arg.serializeStatement())
            .collect(Collectors.joining(", "))
            + ")";
    }

    public List<F> getArgFrames(WorkingMemory<F> workingMemory) {
        return args.stream()
            .map(arg -> {
                F frame = workingMemory.getFrame(arg);
                if (frame == null)
                    throw new ScriptParseException(String.format("Unrecognized symbol `%s'", arg));
                return frame;
            })
            .collect(Collectors.toList());
    }

}
