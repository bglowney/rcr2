package com.rcr2;

import lombok.val;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.stream.Collectors;

import static com.rcr2.Session.DEFAULT_STATE_SERIALIZATION;
import static com.rcr2.Session.IN_SEQUENCE_ALIAS_MARKER;

public class Script {

    private ParseTreeWalker parseTreeWalker = new ParseTreeWalker();

    public <F extends Frame<F>, C extends Context<F,C>> SessionInput<F,C> processStatement(
            @NotNull Session<F,C> session,
            @NotNull String input) {
        return processStatement(session, input, false);
    }

    public <F extends Frame<F>, C extends Context<F,C>> SessionInput<F,C> processStatement(
            @NotNull Session<F,C> session,
            @NotNull String input,
            boolean isInSequence) {
        val lexer = new Rcr2Lexer(new ANTLRInputStream(input));
        val tokenStream = new CommonTokenStream(lexer);
        val parser = new Rcr2Parser(tokenStream);
        val statementContext = parser.statement();
        val statementListener = new StatementListener<F,C>(session);
        parseTreeWalker.walk(statementListener, statementContext);
        val sessionInput = statementListener.getSessionInput();
        // don't accidentally overwrite an existing variable
        if (isInSequence) {
            sessionInput.setAlias(IN_SEQUENCE_ALIAS_MARKER + sessionInput.getAlias().get());
            sessionInput.setArgs(
                sessionInput.getArgs()
                        .stream()
                        .map(arg -> arg.equals(DEFAULT_STATE_SERIALIZATION) ? arg : IN_SEQUENCE_ALIAS_MARKER + arg)
                        .collect(Collectors.toList())
            );
        }
        validate(sessionInput, session, session.context);
        sessionInput.init();
        return sessionInput;
    }

    private void validate(SessionInput sessionInput,
                          Session workingMemory,
                          Context context) {
        val function = sessionInput.getFunction();
        if (function == null)
            throw new ScriptParseException("Statement must include a function");

        if (!context.hasFunction(function))
            throw new ScriptParseException(String.format("Unrecognized function `%s'", function));

        if (context.isPureFunction(function) && !sessionInput.getAlias().isPresent())
            throw new ScriptParseException(String.format("Function `%s' is pure. Its result must be assigned to an alias", function));

        for (Object arg: sessionInput.getArgs()) {
            if (workingMemory.getFrame((String)arg) == null)
                throw new ScriptParseException(String.format("Unrecognized argument `%s'", arg));
            if (!workingMemory.inScope((String)arg))
                throw new ScriptParseException(String.format("Argument `%s' is no longer in scope", arg));
        }

        val actualNumberArgs = sessionInput.getArgs().size();
        val arity = context.arityForFunction(function);
        if (actualNumberArgs != arity)
            throw new ScriptParseException(String.format("Function `%s' requires %s arguments, but was provided %s", function, arity, actualNumberArgs));

    }

}
