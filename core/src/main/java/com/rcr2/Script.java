package com.rcr2;

import lombok.val;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Script {

    private ParseTreeWalker parseTreeWalker = new ParseTreeWalker();

    public <F extends Frame<F>> SessionInput<F> processStatement(@NotNull Context<F> context,
                                                                 @NotNull WorkingMemory<F> workingMemory,
                                                                 @NotNull String input) {
        val lexer = new Rcr2Lexer(new ANTLRInputStream(input));
        val tokenStream = new CommonTokenStream(lexer);
        val parser = new Rcr2Parser(tokenStream);
        val statementContext = parser.statement();
        val statementListener = new StatementListener<F>(context, workingMemory);
        parseTreeWalker.walk(statementListener, statementContext);
        val sessionInput = statementListener.getSessionInput();
        validate(sessionInput, workingMemory, context);
        sessionInput.init();
        return sessionInput;
    }

    private void validate(SessionInput sessionInput,
                          WorkingMemory workingMemory,
                          Context context) {
        val function = sessionInput.getFunction();
        if (function == null)
            throw new ScriptParseException("Statement must include a function");

        if (context.functionForName(function) == null)
            throw new ScriptParseException(String.format("Unrecognized function `%s'", function));

        if (context.functionForName(function) instanceof Function.Pure && !sessionInput.getAlias().isPresent())
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
