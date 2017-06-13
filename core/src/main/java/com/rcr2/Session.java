package com.rcr2;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class Session<F extends Frame<F>, C extends Context<F>> {

    final Script script = new Script();

    protected WorkingMemory<F> workingMemory;
    protected Feedback<F> feedback;
    protected Persistence<F> persistence;
    protected C context;
    protected F currentFrame;

    public Optional<F> imitatedStep(String input) {
        return step(script.processStatement(context, workingMemory, input));
    }

    public Optional<F> imitatorStep() {
        // getFrame the current state key
        val currentState = workingMemory.serializeCurrentState();
        String nextBest = persistence.bestFor(currentState, 2);
        if (nextBest == null)
            nextBest = workingMemory.perturb();
        return step(script.processStatement(context, workingMemory, nextBest));
    }

    private Optional<F> step(SessionInput<F> sessionInput) {
        val result = sessionInput.apply();

        // if the step is to apply a function that changes state
        if (sessionInput.hasSideEffect(context)) {
            // if the side effect does not throw an exception
            // then scan the feedback from the imitated
            if (result.isPresent()) {
                val previous = currentFrame.copy();
                val score = feedback.score(previous, result.get());
                // apply feedback for all value adding elements in working memory
                persistence.update(workingMemory, sessionInput, score);
                // addStep the currentFrame to the result of the side effect
                currentFrame = result.get();
            }
            // otherwise, apply the reverted negative feedback automatically
            else {
                workingMemory.logFailureToMostRecent(sessionInput, feedback.failed());
            }
        }
        // otherwise the step is to make an assignment to an alias in working memory
        else {
            // if truthy, just update the alias in working memory
            // don't need to log feedback
            if (result.isPresent() && !sessionInput.hasSideEffect(context)) {
                workingMemory.addStep(sessionInput, result.get());
            }
            // otherwise just log negative feedback
            else {
                workingMemory.logFailureToMostRecent(sessionInput, feedback.failed());
            }
        }

        return result;
    }

}
