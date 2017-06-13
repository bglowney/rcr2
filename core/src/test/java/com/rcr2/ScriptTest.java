package com.rcr2;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class ScriptTest {

    WorkingMemory<TestFrame> workingMemory;
    Context<TestFrame> context;

    @Before
    public void setup() {
        context = new Context<TestFrame>()
                .withPureFunction("e", 0, args -> Optional.of(new TestFrame()))
                .withPureFunction("f", 2, args -> Optional.of(new TestFrame()))
                .withPureFunction("g", 2, args -> Optional.empty())
                .withPureFunction("h", 1, args -> Optional.of(new TestFrame()));

        workingMemory = new WorkingMemory<>(context, new TestFrame());
        SessionInput<TestFrame> x = new SessionInput<>(context, workingMemory);
        x.setAlias("x");
        x.setFunction("e");
        workingMemory.addStep(x, new TestFrame());
        SessionInput<TestFrame> y = new SessionInput<>(context, workingMemory);
        y.setAlias("y");
        y.setFunction("e");
        workingMemory.addStep(y, new TestFrame());

    }

    @Test
    public void testGoodScript() {
        SessionInput sessionInput = new Script().processStatement(context, workingMemory,"a = f x y;");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 2;
    }

    /**
     * Should throw a parse exception because the function h is not defined
     */
    @Test(expected = ScriptParseException.class)
    public void testUndefinedFunctionScript() {
        SessionInput sessionInput = new Script().processStatement(context, workingMemory,"c = h x y;");
    }

    /**
     * Should throw a parse exception because f is a pure function and the result is not assigned to an alias
     */
    @Test(expected = ScriptParseException.class)
    public void testBadPureFunctionScript() {
        SessionInput sessionInput = new Script().processStatement(context, workingMemory,"f x y;");
    }

    /**
     * Should throw an exception because z is not defined
     */
    @Test(expected = ScriptParseException.class)
    public void testBadReferenceScript() {
        SessionInput sessionInput = new Script().processStatement(context, workingMemory,"b = g x z;");
    }

    /**
     * Should throw an exception because f requires exactly 2 arguments
     */
    @Test(expected = ScriptParseException.class)
    public void testInvalidArguments() {
        SessionInput sessionInput = new Script().processStatement(context, workingMemory,"b = f x;");
    }

    @Test
    public void testParseNestedArgs() {
        SessionInput sessionInput = new Script()
                .processStatement(context, workingMemory, "a = h (text);");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 1;
        assert "text".equals(sessionInput.getArgs().get(0));
    }

    @Test
    public void testParseSingleNestedExpression() {
        // add the expected dependencies to working memory
        workingMemory.addStep(new Script().processStatement(context, workingMemory, "b = h text;"), new TestFrame());

        SessionInput sessionInput = new Script()
                .processStatement(context, workingMemory, "a = h (h (text));");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 1;
        assert "h (text)".equals(sessionInput.getArgs().get(0));
    }

    @Test
    public void testParseMultipleNestedExpression() {
        // add the expected dependencies to working memory
        workingMemory.addStep(new Script().processStatement(context, workingMemory, "b = h text;"), new TestFrame());
        workingMemory.addStep(new Script().processStatement(context, workingMemory, "c = f text text;"), new TestFrame());

        SessionInput sessionInput = new Script()
                .processStatement(context, workingMemory, "a = h (h (text), f (text, text));");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 2;
        assert "h (text)".equals(sessionInput.getArgs().get(0));
        assert "f (text, text)".equals(sessionInput.getArgs().get(1));
    }

}
