package com.rcr2;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkingMemory<F extends Frame<F>> {

    public static final String DEFAULT_STATE_SERIALIZATION = "text";
    public static final int MAX_SIZE_IN_SCOPE = 4;
    static final int MAX_ARCHIVE_SIZE = 10;

    @RequiredArgsConstructor
    public static class Entry<F extends Frame<F>> implements Comparable<Entry> {
        @NotNull @Getter final SessionInput<F> sessionInput;
        @NotNull @Getter final F result;
        @Getter final int step;
        @Getter final List<Failure<F>> failures = new ArrayList<>();

        @AllArgsConstructor
        public static class Failure<F extends Frame<F>> {
            @NotNull @Getter final SessionInput<F> failedInput;
            @Getter final int feedback;
        }

        @Override
        public int compareTo(Entry o) {
            return -1 * (step - o.step); // Sort descending
        }

    }


    @Getter @NonFinal int currentStep = 0;

    @Getter final Map<String,Entry<F>> entriesByAlias = new HashMap<>();
    final Map<String,Entry<F>> entriesBySerialization = new HashMap<>();
    @Getter @Setter F text;
    Context<F> context;
    long perturbIndex = 0;

    public WorkingMemory(@NonNull Context<F> context, F text) {
        this.text = text;
        this.context = context;
        Entry<F> textEntry = new Entry<>(new TextInput<>(context, this), text, currentStep);
        this.entriesBySerialization.put(DEFAULT_STATE_SERIALIZATION, textEntry);
        this.entriesByAlias.put(DEFAULT_STATE_SERIALIZATION, textEntry);
    }

    private Map.Entry<String,Entry<F>> mostRecentEntry() {
        return this.entriesByAlias
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .findFirst()
                .orElse(null);
    }

    private Map.Entry<String,Entry<F>> leastRecentEntry(Map<String,Entry<F>> map) {
        return map
                .entrySet()
                .stream()
                .sorted(Comparator.comparingInt(a -> a.getValue().getStep())) // reverse order
                .findFirst()
                .orElse(null);
    }

    private Entry<F> getEntry(String arg) {
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

    public void logFailureToMostRecent(SessionInput<F> failedInput, int feedback) {
        mostRecentEntry().getValue().failures.add(new Entry.Failure<F>(failedInput, feedback));
    }

    public void addStep(SessionInput<F> sessionInput, F result) {
        val entry = new Entry<F>(sessionInput, result, currentStep++);
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

//    private void tryToRemoveEntry(Map<String,Entry<F>> map) {
//        try {
//            map.entrySet()
//                    .stream()
//                    .sorted(Map.Entry.comparingByValue())
//                    .limit(MAX_SIZE_IN_SCOPE)
//                    .map(Map.Entry::getValue)
//                    .forEach(entry -> entry.getSessionInput().serializeStatement());
//
//            map.remove(leastRecentEntry(map).getKey());
//
//        } catch (NullPointerException e) {
//            System.out.println("Couldn't remove oldest entry. Entries in scope still depend on it");
//        }
//    }

    public String serializeCurrentState() {
        if (entriesByAlias.isEmpty()) {
            return DEFAULT_STATE_SERIALIZATION;
        } else {
            val current = entriesByAlias.get(mostRecentEntry().getKey()).sessionInput;
            return context.find(current).serialize();
        }
    }

    protected static class TextInput<F extends Frame<F>> extends SessionInput<F> {
        public TextInput(Context<F> context, WorkingMemory<F> workingMemory) {
            super(context, workingMemory);
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

    public SessionInput<F> getInput(String arg) {
        if (arg.equals(DEFAULT_STATE_SERIALIZATION))
            return new TextInput<>(context, this);

        val entry = getEntry(arg);

        return entry != null ? entry.sessionInput: null;
    }

    public F getFrame(String arg) {
        if (DEFAULT_STATE_SERIALIZATION.equals(arg))
            return text;

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
        if (entry.function instanceof Function.Pure) {
            scriptBuilder.append("__");
            scriptBuilder.append(this.perturbIndex++);
            scriptBuilder.append(" = ");
        }
        scriptBuilder.append(entry.name);

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
