package com.rcr2;

import lombok.*;
import lombok.experimental.NonFinal;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@NoArgsConstructor
public class Session<F extends Frame<F>, C extends Context<F,C>> {

    final Script script = new Script();

    protected Feedback<F> feedback;
    protected Persistence<F,C> persistence;
    protected F currentFrame;
    protected FrameProvider<F> frameProvider;
    protected C context;

    @Getter @NonFinal
    int currentStep = 0;

    @Getter final Map<String,Entry<F,C>> entriesByAlias = new HashMap<>();
    final Map<String,Entry<F,C>> entriesBySerialization = new HashMap<>();
    @Getter @Setter F text;
    long perturbIndex = 0;

    public Session(F text, Feedback<F> feedback, C context, Persistence<F,C> persistence, FrameProvider<F> frameProvider) {
        this.currentFrame = text;
        this.feedback = feedback;
        this.context = context;
        this.persistence = persistence;
        this.frameProvider = frameProvider;
        Entry<F,C> textEntry = new Entry<>(new TextInput<>(this), text, currentStep, false);
        this.entriesBySerialization.put(DEFAULT_STATE_SERIALIZATION, textEntry);
        this.entriesByAlias.put(DEFAULT_STATE_SERIALIZATION, textEntry);
    }

    public Optional<F> imitatedStep(String input) {
        return step(script.processStatement(this, input), false);
    }

    Optional<F> inSequenceStep(Function.Sequence<F,C> sequence) {
        for (val s: sequence.scripts) {
            val result = step(script.processStatement(this, s + ";", true), true);
            if (!result.isPresent()) {
                removeInSequenceEntries();
                return Optional.empty();
            }
        }

        val wrappedComponents = new ArrayList<F>();
        for (val component : sequence.components)
            wrappedComponents.add(getFrame(IN_SEQUENCE_ALIAS_MARKER + component));

        sequence.baseFrame.wrap(wrappedComponents);
        removeInSequenceEntries();
        return Optional.of(sequence.baseFrame);
    }

    public Optional<F> imitatorStep() {
        // getFrame the current state key
        val currentState = this.serializeCurrentState();
        String nextBest = persistence.bestFor(currentState, 2);
        if (nextBest == null)
            nextBest = this.perturb();
        return step(script.processStatement(this, nextBest), false);
    }

    public static final String IN_SEQUENCE_ALIAS_MARKER = "__";

    private Optional<F> step(SessionInput<F,C> sessionInput, boolean inSequence) {
        val result = sessionInput.apply(this, frameProvider.newFrame());

        // if the step is to apply a function that changes state
        if (sessionInput.hasSideEffect(context)) {
            // if the side effect does not throw an exception
            // then scan the feedback from the imitated
            if (result.isPresent()) {
                val previous = currentFrame.copy();
                val score = feedback.score(previous, result.get());
                // apply feedback for all value adding elements in working memory
                persistence.update(this, sessionInput, score);
                // addStep the currentFrame to the result of the side effect
                currentFrame = result.get();
            }
            // otherwise, apply the reverted negative feedback automatically
            else
                this.logFailureToMostRecent(sessionInput, feedback.failed());
        }
        // otherwise the step is to make an assignment to an alias in working memory
        else {
            // if truthy, just update the alias in working memory
            // don't need to log feedback
            if (result.isPresent() && !sessionInput.hasSideEffect(context))
                this.addStep(sessionInput, result.get(), inSequence);
            // otherwise just log negative feedback
            else
                this.logFailureToMostRecent(sessionInput, feedback.failed());
        }

        return result;
    }

    private void removeInSequenceEntries() {
        val toRemove = new ArrayList<String>();
        for (val entry: entriesByAlias.values()) {
            if (entry.isInSequence() && entry.getSessionInput().getAlias().isPresent())
                toRemove.add(entry.getSessionInput().getAlias().get());
        }
        for (val key : toRemove)
            entriesByAlias.remove(key);
    }

    public static final String DEFAULT_STATE_SERIALIZATION = "text";
    public static final int MAX_SIZE_IN_SCOPE = 4;
    static final int MAX_ARCHIVE_SIZE = 10;

    @RequiredArgsConstructor
    public static class Entry<F extends Frame<F>, C extends Context<F,C>> implements Comparable<Entry> {
        @NotNull
        @Getter
        final SessionInput<F,C> sessionInput;
        @NotNull @Getter final F result;
        @Getter final int step;
        @Getter final List<Failure<F,C>> failures = new ArrayList<>();
        @Getter final boolean inSequence;

        @AllArgsConstructor
        public static class Failure<F extends Frame<F>, C extends Context<F,C>> {
            @NotNull @Getter final SessionInput<F,C> failedInput;
            @Getter final int feedback;
        }

        @Override
        public int compareTo(Entry o) {
            return -1 * (step - o.step); // Sort descending
        }

    }

    private Map.Entry<String,Entry<F,C>> mostRecentEntry() {
        return this.entriesByAlias
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .findFirst()
                .orElse(null);
    }

    private Map.Entry<String,Entry<F,C>> leastRecentEntry(Map<String,Entry<F,C>> map) {
        return map
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getValue().getStep())) // reverse order
                .findFirst()
                .orElse(null);
    }

    private Entry<F,C> getEntry(String arg) {
        return entriesByAlias.getOrDefault(arg, entriesBySerialization.get(arg));
    }

    public String serializePrevious(String arg) {
        val entry = getEntry(arg);

        if (entry != null) {
            String serialization = this.entriesByAlias.values()
                    .stream()
                    .filter(e -> e.getStep() <= entry.getStep() && e.getStep() > currentStep - MAX_SIZE_IN_SCOPE)
                    .map(e -> e.getSessionInput().serializeStatement())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.joining(","));

            return serialization == null || serialization.equals("")
                    ? DEFAULT_STATE_SERIALIZATION
                    : serialization;
        }

        return null;
    }

    public void logFailureToMostRecent(SessionInput<F,C> failedInput, int feedback) {
        mostRecentEntry().getValue().failures.add(new Entry.Failure<>(failedInput, feedback));
    }

    public void addStep(SessionInput<F,C> sessionInput, F result, boolean inSequence) {
        val entry = new Entry<F,C>(sessionInput, result, currentStep, inSequence);

        if (!inSequence)
            currentStep++;

        entriesByAlias.put(entry.sessionInput.alias, entry);
        entriesBySerialization.put(entry.sessionInput.serializeStatement(), entry);
        if (entriesByAlias.size() > MAX_ARCHIVE_SIZE) {
//            tryToRemoveEntry(entriesByAlias);
            entriesByAlias.remove(leastRecentEntry(entriesByAlias).getKey());
        }
        if (entriesBySerialization.size() > MAX_ARCHIVE_SIZE) {
//            tryToRemoveEntry(entriesBySerialization);
            entriesBySerialization.remove(leastRecentEntry(entriesBySerialization).getKey());
        }
    }

    public String serializeCurrentState() {
        if (entriesByAlias.isEmpty()) {
            return DEFAULT_STATE_SERIALIZATION;
        } else {
            val current = entriesByAlias.get(mostRecentEntry().getKey()).sessionInput;
            return context.find(current).serialize();
        }
    }

    protected static class TextInput<F extends Frame<F>, C extends Context<F,C>> extends SessionInput<F,C> {
        public TextInput(Session<F,C> session) {
            super(session);
            this.alias = DEFAULT_STATE_SERIALIZATION;
        }

        @Override
        public SortedSet<String> dependsOn() {
            return Collections.emptySortedSet();
        }

        @Override
        public String serializeStatement() {
            return DEFAULT_STATE_SERIALIZATION;
        }
    }

    public boolean inScope(String arg) {
        // text is always available
        if (DEFAULT_STATE_SERIALIZATION.equals(arg))
            return true;

        val entry = getEntry(arg);
        return entry != null && entry.getStep() >= currentStep - MAX_SIZE_IN_SCOPE;
    }

    public SessionInput<F,C> getInput(String arg) {
        if (arg.equals(DEFAULT_STATE_SERIALIZATION))
            return new TextInput<>(this);

        val entry = getEntry(arg);

        return entry != null ? entry.sessionInput: null;
    }

    public F getFrame(String arg) {
        if (DEFAULT_STATE_SERIALIZATION.equals(arg))
            return currentFrame;

        val entry = getEntry(arg);

        return entry != null ? entry.result : null;
    }

    public String perturb() {

        val optionalEntry = context
                .allFunctions()
                .stream()
                .skip(ThreadLocalRandom.current().nextInt(0, context.allFunctions().size()))
                .findFirst();

        if(!optionalEntry.isPresent())
            throw new IllegalStateException("Cannot perturb. Context contains no applicable functions");

        val entry = optionalEntry.get();

        val scriptBuilder = new StringBuilder();
        if (entry.getFunction() instanceof Function.Pure) {
            scriptBuilder.append("__");
            scriptBuilder.append(this.perturbIndex++);
            scriptBuilder.append(" = ");
        }
        scriptBuilder.append(entry.getName());

        val candidateArgs = this.entriesByAlias.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(MAX_SIZE_IN_SCOPE)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (int a = 0; a < entry.arity; a ++) {
            scriptBuilder.append(" ");
            String arg = candidateArgs
                    .stream()
                    .skip(ThreadLocalRandom.current()
                            .nextInt(0, candidateArgs.size()))
                    .findFirst()
                    .orElse(DEFAULT_STATE_SERIALIZATION);
            scriptBuilder.append(arg);
        }

        scriptBuilder.append(";");

        return scriptBuilder.toString();
    }

    public String display() {
        return this.entriesByAlias.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(MAX_SIZE_IN_SCOPE)
                .map(Map.Entry::getValue)
                .map(entry -> entry.getStep()
                        + "\t" + entry.getSessionInput().getAlias().get()
                        + "\t" + entry.getSessionInput().serializeStatement())
                .collect(Collectors.joining("\n"));
    }

}
