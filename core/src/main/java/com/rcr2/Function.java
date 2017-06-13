package com.rcr2;

import java.util.List;
import java.util.Optional;

@FunctionalInterface
public interface Function<F extends Frame<F>> {

    Optional<F> apply(List<F> args);

    interface Pure<F extends Frame<F>> extends Function<F> {}
    interface SideEffect<F extends Frame<F>> extends Function<F> {}

}
