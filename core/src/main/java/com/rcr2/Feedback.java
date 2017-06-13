package com.rcr2;

public interface Feedback<F extends Frame<F>> {
    int score(F previous, F currentFrame);

    int failed();
}
