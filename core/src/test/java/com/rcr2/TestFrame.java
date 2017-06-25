package com.rcr2;

import lombok.Getter;

import java.util.List;

public class TestFrame implements Frame<TestFrame> {

    @Getter private List<TestFrame> others;

    @Override
    public TestFrame copy() {
        return new TestFrame();
    }

    @Override
    public void wrap(List<TestFrame> others) {
        this.others = others;
    }
}
