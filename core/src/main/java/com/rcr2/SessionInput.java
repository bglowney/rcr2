package com.rcr2;

import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SessionInput<F extends Frame<F>, C extends Context<F,C>> {

    @Getter @Setter
    private String function;
    @Getter @Setter
    protected List<String> args = new ArrayList<>();
    @Setter
    protected String alias;
    protected Optional<F> result;

    protected final Session<F,C> session;
    protected final Context<F,C> context;

    private String serialization;

    public SessionInput(Session<F,C> session) {
        this.session = session;
        this.context = session.context;
    }

    public void init() {
        this.serialization = this.serializeStatement();
    }

    public void addArg(String arg) {
        args.add(arg);
    }

    public boolean hasSideEffect(Context<F,C> context) {
        return !context.isPureFunction(function);
    }

    /**
     * Get the args of this sessionInput in serialized form,
     * and concatenate with the args of all children
     */
    public SortedSet<String> dependsOn() {
        return Stream.concat(args.stream().map(arg -> session.getInput(arg).serializeStatement()),
                      args.stream().flatMap(arg -> session.getInput(arg).dependsOn().stream()))
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<F> apply(Session<F,C> session, F baseFrame) {
        if (this.result == null)
            this.result = context.functionForName(function, session, baseFrame).apply(getArgFrames());
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
            this.result = this.apply(session, session.frameProvider.newFrame());

        final String annotation = result
                .map(f -> f instanceof EmptyFrame ? Frame.EMPTY_ANNOTATION : "")
                .orElse(Frame.FAILED_ANNOTATION);

        return annotation + this.function + " (" +
            args.stream()
            .map(arg -> session.getInput(arg))
            .map(arg -> arg.serializeStatement())
            .collect(Collectors.joining(", "))
            + ")";
    }

    public List<F> getArgFrames() {
        return args.stream()
            .map(arg -> {
                F frame = session.getFrame(arg);
                if (frame == null)
                    throw new ScriptParseException(String.format("Unrecognized symbol `%s'", arg));
                return frame;
            })
            .collect(Collectors.toList());
    }

}
