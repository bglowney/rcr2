package com.rcr2;

public interface Frame<F extends Frame<F>> {

    public static final String EMPTY_ANNOTATION = "~";
    public static final String FAILED_ANNOTATION = "!";

    public F copy();

}
