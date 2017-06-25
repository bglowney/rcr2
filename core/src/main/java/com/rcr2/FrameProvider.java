package com.rcr2;

@FunctionalInterface
public interface FrameProvider<F extends Frame<F>> {
    F newFrame();
}
