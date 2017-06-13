package com.rcr2.impl;

import com.rcr2.Frame;

public interface DisplayableFrame<F extends DisplayableFrame<F>> extends Frame<F> {

    String display();

}
