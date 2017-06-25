package com.rcr2;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;

public class WorkingMemoryTest {

    Script script;
    TestContext context;

    @Before
    public void setup() {
        script = new Script();
        context = new TestContext(new TestSequenceProvider());
        context.withPureFunction("f", 1, args -> Optional.of(new TestFrame()));
        context.withPureFunction("g", 1, args -> Optional.empty());
        context.withPureFunction("h", 2, args -> Optional.of(new TestFrame()));
    }

    @Test
    @SuppressWarnings("unchecked") // ignore generic conversions just for contrived test state
    public void testSessionInputSerialization() {
        Session<TestFrame,TestContext> session = new TestSession(context);
        SessionInput<TestFrame,TestContext> input1 = script.processStatement(session, "a = f text;");
        SortedSet<String> dependenciesSet = input1.dependsOn();
        List<String> dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 1;
        assert dependenciesList.get(0).equals("text");
        session.addStep(input1, new TestFrame(), false);

        SessionInput<TestFrame,TestContext> input2 = script.processStatement(session, "b = f a;");
        dependenciesSet = input2.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 2;
        assert dependenciesList.get(0).equals("f (text)");
        assert dependenciesList.get(1).equals("text");
        session.addStep(input2, new TestFrame(), false);

        SessionInput<TestFrame,TestContext> input3 = script.processStatement(session, "c = f b;");
        dependenciesSet = input3.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 3;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("text");
        session.addStep(input3, new TestFrame(), false);

        SessionInput<TestFrame,TestContext> input4 = script.processStatement(session, "d = h a b;");
        dependenciesSet = input4.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 3;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("text");
        session.addStep(input4, new TestFrame(), false);

        SessionInput<TestFrame,TestContext> input5 = script.processStatement(session, "e = f d;");
        dependenciesSet = input5.dependsOn();
        dependenciesList = new ArrayList<>(dependenciesSet);
        assert dependenciesSet.size() == 4;
        assert dependenciesList.get(0).equals("f (f (text))");
        assert dependenciesList.get(1).equals("f (text)");
        assert dependenciesList.get(2).equals("h (f (text), f (f (text)))");
        assert dependenciesList.get(3).equals("text");
        session.addStep(input5, new TestFrame(), false);

    }

    @Test
    public void testPerturb() {
        Session<TestFrame,TestContext> session = new TestSession(context);
        String perturbScript = session.perturb();

        assert perturbScript.equals("__0 = f text;")
                || perturbScript.equals("__0 = g text;")
                || perturbScript.equals("__0 = h text text;");

        SessionInput<TestFrame,TestContext> result = script.processStatement(session, perturbScript);

        session.addStep(result, new TestFrame(), false);

        // all functions are pure so we can reassign endlessly
        // we should theoretically be able to run this loop until we run out of memory
        for (int i = 0; i < 100; i++) {
            perturbScript = session.perturb();
            result = script.processStatement(session, perturbScript);
            session.addStep(result, new TestFrame(), false);
        }

        System.out.println("Working memory final size " + session.getEntriesByAlias().size());

    }
}
