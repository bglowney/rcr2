package com.rcr2;

import java.util.List;

public interface Frame<F extends Frame<F>> {

    String EMPTY_ANNOTATION = "~";
    String FAILED_ANNOTATION = "!";

    F copy();
    void wrap(List<F> others);

}
