package com.rcr2;

import com.rcr2.impl.InMemoryPersistence;
import com.rcr2.impl.InMemorySequenceProvider;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Optional;

public class SessionTest {

    private Session<TestFrame,TestContext> session;

    @Before
    public void setup() {
        final TestContext context = new TestContext(new TestSequenceProvider());
        context.withPureFunction("f", 1, args -> Optional.of(new TestFrame()));
        context.withSideEffect("g", 0, args -> Optional.of(new TestFrame()));
        context.withSideEffect("h", 0, args -> Optional.empty());
        val persistence = new InMemoryPersistence<TestFrame,TestContext>();

        session = new Session<>(
            new TestFrame(),
            new TestFeedback(),
            context,
            persistence,
            TestFrame::new
        );
    }

    @Test
    public void testSession() {
        assert session.getCurrentStep() == 0;
        session.imitatedStep("a = f text;");
        session.imitatedStep("b = f a;");
        session.imitatedStep("c = f b;");
        session.imitatedStep("d = f c;");
        session.imitatedStep("e = f d;");
        assert session.getCurrentStep() == 5;
        assert session.getEntriesByAlias().size() <= Session.MAX_SIZE_IN_SCOPE * 2;
        assert !session.inScope("a");
        assert session.inScope("b");
        session.imitatedStep("h;");
        session.imitatedStep("g;");
    }

    @Test
    public void testSessionWithSequence() {
        assert session.getCurrentStep() == 0;
        session.imitatedStep("s1 = seq1;");
        assert session.getFrame("a") == null;
        assert session.getFrame("b") == null;
        assert session.getFrame("c") == null;
        assert session.getFrame("s1") != null;
        assert session.getCurrentStep() == 1;
        session.imitatedStep("s2 = seq1;");
        assert session.getCurrentStep() == 2;
        assert session.getFrame("s2") != null;
        assert session.getFrame("s2").getOthers() != null;
        assert session.getFrame("s2").getOthers().size() == 1;
    }

    @Test
    public void testSessionWithNestedSequence() {
        assert session.getCurrentStep() == 0;
        session.imitatedStep("s2 = seq2;");
        assert session.getCurrentStep() == 1;
        assert session.getFrame("s2") != null;
        assert session.getFrame("s2").getOthers() != null;
        assert session.getFrame("s2").getOthers().size() == 2;
    }
}
