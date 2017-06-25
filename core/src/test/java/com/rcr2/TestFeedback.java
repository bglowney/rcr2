package com.rcr2;

public class TestFeedback implements Feedback<TestFrame> {

    @Override
    public int score(TestFrame previous, TestFrame currentFrame) {
        return 1;
    }

    @Override
    public int failed() {
        return -1;
    }

}
