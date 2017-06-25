package com.rcr2;

import com.rcr2.impl.InMemoryPersistence;

public class TestSession extends Session<TestFrame,TestContext> {

    public TestSession(TestContext context) {
        super(
            new TestFrame(),
            new TestFeedback(),
            context,
            new InMemoryPersistence<TestFrame, TestContext>(),
            TestFrame::new
        );
    }

}
