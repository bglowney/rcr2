package com.rcr2;

public class TestFrame implements Frame<TestFrame> {
    @Override
    public TestFrame copy() {
        return new TestFrame();
    }
}
