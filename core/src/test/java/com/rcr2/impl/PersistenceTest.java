package com.rcr2.impl;

import com.rcr2.*;
import com.rcr2.impl.InMemoryPersistence.FeedbackStats;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.rcr2.WorkingMemory.DEFAULT_STATE_SERIALIZATION;

public class PersistenceTest {

    Script script;
    Context<TestFrame> context;
    InMemoryPersistence<TestFrame> persistence;

    @Before
    public void setup() {
        script = new Script();
        context = new Context<TestFrame>()
                .withPureFunction("f", 1, args -> Optional.of(new TestFrame()))
                .withPureFunction("g", 1, args -> Optional.of(new TestFrame()))
                .withPureFunction("h", 2, args -> Optional.of(new TestFrame()));
        persistence = new InMemoryPersistence<>();
    }

    @Test
    public void testInMemoryPersistence() {
        WorkingMemory<TestFrame> workingMemory = new WorkingMemory<>(context, new TestFrame());
        SessionInput<TestFrame> input1 = script.processStatement(context, workingMemory, "a = f text;");
        workingMemory.addStep(input1, new TestFrame());
        SessionInput<TestFrame> input2 = script.processStatement(context, workingMemory,"b = f a;");
        workingMemory.addStep(input2, new TestFrame());

        persistence.update(workingMemory, 1);

        Map<String,Persistence.FeedbackStats> m = persistence.data.get(DEFAULT_STATE_SERIALIZATION);
        assert m != null;
        Persistence.FeedbackStats stats = m.get(input1.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 1;
        assert ((FeedbackStats) stats).cumulative == 1;

        m = persistence.data.get(workingMemory.serializePrevious(input1.getAlias().get()));
        assert m != null;
        stats = m.get(input2.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 1;
        assert ((FeedbackStats) stats).cumulative == 1;

        // simulate case that input1 and input 2 were applied again in a new session
        workingMemory = new WorkingMemory<>(context, new TestFrame());
        workingMemory.addStep(input1, new TestFrame());
        workingMemory.addStep(input2, new TestFrame());

        persistence.update(workingMemory, 1);

        m = persistence.data.get(DEFAULT_STATE_SERIALIZATION);
        assert m != null;
        stats = m.get(input1.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 2;
        assert ((FeedbackStats) stats).cumulative == 2;

        m = persistence.data.get(workingMemory.serializePrevious(input1.getAlias().get()));
        assert m != null;
        stats = m.get(input2.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 2;
        assert ((FeedbackStats) stats).cumulative == 2;
    }

    @Test
    public void testBestFor() {
        WorkingMemory<TestFrame> workingMemory = new WorkingMemory<>(context, new TestFrame());
        workingMemory.addStep(script.processStatement(context, workingMemory, "a = f text;"), new TestFrame());
        persistence.update(workingMemory, 1);

        workingMemory = new WorkingMemory<>(context, new TestFrame());
        workingMemory.addStep(script.processStatement(context, workingMemory, "a = f text;"), new TestFrame());
        persistence.update(workingMemory, 1);

        String dependencies = "text";

        String bestScript = persistence.bestFor(dependencies, 3);

        // with 3 minimum observations we should not have any reliable best
        assert bestScript == null;

        workingMemory = new WorkingMemory<>(context, new TestFrame());
        workingMemory.addStep(script.processStatement(context, workingMemory, "a = g text;"), new TestFrame());
        persistence.update(workingMemory, 1);

        workingMemory = new WorkingMemory<>(context, new TestFrame());
        workingMemory.addStep(script.processStatement(context, workingMemory, "a = g text;"), new TestFrame());
        persistence.update(workingMemory, 2);

        // we have applied f and g to text twice respectively
        // because cumulative feedback for g was greater, we would expect the best
        // to be `g (text)`
        bestScript = persistence.bestFor(dependencies, 2);

        assert bestScript.equals("g (text)");

        workingMemory.addStep(script.processStatement(context, workingMemory, "b = " + bestScript + ";") , new TestFrame());
        SessionInput sessionInput = workingMemory.getInput("b");

        assert sessionInput != null;

    }
}
