package com.rcr2;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

public class WorkingMemoryTest {

    Script script;
    Context<TestFrame> context;

    @Before
    public void setup() {
        script = new Script();
        context = new Context<TestFrame>()
                .withPureFunction("f", 1, args -> Optional.of(new TestFrame()))
                .withPureFunction("g", 1, args -> Optional.empty())
                .withPureFunction("h", 2, args -> Optional.of(new TestFrame()));
    }

    @Test
    @SuppressWarnings("unchecked") // ignore generic conversions just for contrived test state
    public void testSessionInputSerialization() {
        WorkingMemory<TestFrame> workingMemory = new WorkingMemory<>(context, new TestFrame());
        SessionInput<TestFrame> input1 = script.processStatement(context, workingMemory, "a = f text;");
        SortedSet<String> dependenciesSet = input1.dependsOn();
        List<String> dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 1;
        assert dependenciesList.get(0).equals("text");
        workingMemory.addStep(input1, new TestFrame());

        SessionInput<TestFrame> input2 = script.processStatement(context, workingMemory, "b = f a;");
        dependenciesSet = input2.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 2;
        assert dependenciesList.get(0).equals("f (text)");
        assert dependenciesList.get(1).equals("text");
        workingMemory.addStep(input2, new TestFrame());

        SessionInput<TestFrame> input3 = script.processStatement(context, workingMemory, "c = f b;");
        dependenciesSet = input3.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 3;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("text");
        workingMemory.addStep(input3, new TestFrame());

        SessionInput<TestFrame> input4 = script.processStatement(context, workingMemory, "d = h a b;");
        dependenciesSet = input4.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 3;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("text");
        workingMemory.addStep(input4, new TestFrame());

        SessionInput<TestFrame> input5 = script.processStatement(context, workingMemory, "e = f d;");
        dependenciesSet = input5.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 4;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("h (f (text), f (f (text)))");
        assert dependenciesList.get(3).equals("text");
        workingMemory.addStep(input5, new TestFrame());

    }

    @Test
    public void testPerturb() {
        WorkingMemory<TestFrame> workingMemory = new WorkingMemory<>(context, new TestFrame());
        String perturbScript = workingMemory.perturb();

        assert perturbScript.equals("__0 = f text;")
                || perturbScript.equals("__0 = g text;")
                || perturbScript.equals("__0 = h text text;");

        SessionInput<TestFrame> result = script.processStatement(context, workingMemory, perturbScript);

        workingMemory.addStep(result, new TestFrame());

        // all functions are pure so we can reassign endlessly
        // we should theoretically be able to run this loop until we run out of memory
        for (int i = 0; i < 100; i++) {
            perturbScript = workingMemory.perturb();
            result = script.processStatement(context, workingMemory, perturbScript);
            workingMemory.addStep(result, new TestFrame());
        }

        System.out.println("Working memory final size " + workingMemory.getEntriesByAlias().size());

    }
}
