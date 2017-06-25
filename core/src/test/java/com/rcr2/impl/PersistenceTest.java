package com.rcr2.impl;

import com.rcr2.*;
import com.rcr2.impl.InMemoryPersistence.FeedbackStats;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.rcr2.Session.DEFAULT_STATE_SERIALIZATION;

public class PersistenceTest {

    Script script;
    TestContext context;
    InMemoryPersistence<TestFrame,TestContext> persistence;

    @Before
    public void setup() {
        script = new Script();
        context = new TestContext(new InMemorySequenceProvider<>());
        context.withPureFunction("f", 1, args -> Optional.of(new TestFrame()));
        context.withPureFunction("g", 1, args -> Optional.of(new TestFrame()));
        context.withPureFunction("h", 2, args -> Optional.of(new TestFrame()));
        persistence = new InMemoryPersistence<>();
    }

    @Test
    public void testInMemoryPersistence() {
        Session<TestFrame,TestContext> session = new TestSession(context);
        val input1 = script.processStatement(session, "a = f text;");
        session.addStep(input1, new TestFrame(), false);
        val input2 = script.processStatement(session,"b = f a;");
        session.addStep(input2, new TestFrame(), false);

        persistence.update(session, 1);

        Map<String,Persistence.FeedbackStats> m = persistence.data.get(DEFAULT_STATE_SERIALIZATION);
        assert m != null;
        Persistence.FeedbackStats stats = m.get(input1.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 1;
        assert ((FeedbackStats) stats).cumulative == 1;

        m = persistence.data.get(session.serializePrevious(input1.getAlias().get()));
        assert m != null;
        stats = m.get(input2.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 1;
        assert ((FeedbackStats) stats).cumulative == 1;

        // simulate case that input1 and input 2 were applied again in a new session
        session = new TestSession(context);
        session.addStep(input1, new TestFrame(), false);
        session.addStep(input2, new TestFrame(), false);

        persistence.update(session, 1);

        m = persistence.data.get(DEFAULT_STATE_SERIALIZATION);
        assert m != null;
        stats = m.get(input1.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 2;
        assert ((FeedbackStats) stats).cumulative == 2;

        m = persistence.data.get(session.serializePrevious(input1.getAlias().get()));
        assert m != null;
        stats = m.get(input2.serializeStatement());
        assert stats != null;
        assert stats.getCount() == 2;
        assert ((FeedbackStats) stats).cumulative == 2;
    }

    @Test
    public void testBestFor() {
        Session<TestFrame,TestContext> session = new TestSession(context);
        session.addStep(script.processStatement(session, "a = f text;"), new TestFrame(), false);
        persistence.update(session, 1);

        session = new TestSession(context);
        session.addStep(script.processStatement(session, "a = f text;"), new TestFrame(),false);
        persistence.update(session, 1);

        String dependencies = "text";

        String bestScript = persistence.bestFor(dependencies, 3);

        // with 3 minimum observations we should not have any reliable best
        assert bestScript == null;

        session = new TestSession(context);
        session.addStep(script.processStatement(session, "a = g text;"), new TestFrame(), false);
        persistence.update(session, 1);

        session = new TestSession(context);
        session.addStep(script.processStatement(session, "a = g text;"), new TestFrame(), false);
        persistence.update(session, 2);

        // we have applied f and g to text twice respectively
        // because cumulative feedback for g was greater, we would expect the best
        // to be `g (text)`
        bestScript = persistence.bestFor(dependencies, 2);

        assert bestScript.equals("g (text)");

        session.addStep(script.processStatement(session, "b = " + bestScript + ";") , new TestFrame(),false);
        SessionInput sessionInput = session.getInput("b");

        assert sessionInput != null;

    }
}
