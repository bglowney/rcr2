package com.rcr2;

import com.rcr2.impl.InMemoryPersistence;
import org.junit.Test;

import java.util.Optional;

public class SessionTest {

    @Test
    public void testSession() {
        TestFrame currentFrame = new TestFrame();
        final Context<TestFrame> context = new Context<TestFrame>()
                .withPureFunction("f", 1, args -> Optional.of(new TestFrame()))
                .withSideEffect("g", 0, args -> Optional.of(new TestFrame()))
                .withSideEffect("h", 0, args -> Optional.empty());
        final WorkingMemory<TestFrame> workingMemory = new WorkingMemory<>(context, currentFrame);
        final InMemoryPersistence<TestFrame> persistence = new InMemoryPersistence<>();

        final Session<TestFrame,Context<TestFrame>> session = new Session<>(
                workingMemory,
                new Feedback<TestFrame>() {
                    @Override
                    public int score(TestFrame previous, TestFrame currentFrame) {
                        return 1;
                    }

                    @Override
                    public int failed() {
                        return -1;
                    }
                },
                persistence,
                context,
                currentFrame
        );

        assert workingMemory.getCurrentStep() == 0;
        session.imitatedStep("a = f text;");
        session.imitatedStep("b = f a;");
        session.imitatedStep("c = f b;");
        session.imitatedStep("d = f c;");
        session.imitatedStep("e = f d;");
        assert workingMemory.getCurrentStep() == 5;
        assert workingMemory.getEntriesByAlias().size() <= WorkingMemory.MAX_SIZE_IN_SCOPE * 2;
        assert !workingMemory.inScope("a");
        assert workingMemory.inScope("b");
        session.imitatedStep("h;");
        session.imitatedStep("g;");

    }
}
