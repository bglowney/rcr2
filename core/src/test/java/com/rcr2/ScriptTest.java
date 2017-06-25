package com.rcr2;

import com.rcr2.impl.InMemoryPersistence;
import com.rcr2.impl.InMemorySequenceProvider;
import lombok.val;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class ScriptTest {

    Session<TestFrame,TestContext> session;
    TestContext context;

    @Before
    public void setup() {
        context = new TestContext(new InMemorySequenceProvider<>());
        context.withPureFunction("e", 0, args -> Optional.of(new TestFrame()));
        context.withPureFunction("f", 2, args -> Optional.of(new TestFrame()));
        context.withPureFunction("g", 2, args -> Optional.empty());
        context.withPureFunction("h", 1, args -> Optional.of(new TestFrame()));

        session = new Session<>(
                new TestFrame(),
                new TestFeedback(),
                context,
                new InMemoryPersistence<>(),
                TestFrame::new
        );

        val x = new SessionInput<TestFrame,TestContext>(session);
        x.setAlias("x");
        x.setFunction("e");
        session.addStep(x, new TestFrame(), false);
        val y = new SessionInput<TestFrame,TestContext>(session);
        y.setAlias("y");
        y.setFunction("e");
        session.addStep(y, new TestFrame(), false);

    }

    @Test
    public void testGoodScript() {
        SessionInput sessionInput = new Script().processStatement(session,"a = f x y;");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 2;
    }

    /**
     * Should throw a parse exception because the function h is not defined
     */
    @Test(expected = ScriptParseException.class)
    public void testUndefinedFunctionScript() {
        SessionInput sessionInput = new Script().processStatement(session,"c = h x y;");
    }

    /**
     * Should throw a parse exception because f is a pure function and the result is not assigned to an alias
     */
    @Test(expected = ScriptParseException.class)
    public void testBadPureFunctionScript() {
        SessionInput sessionInput = new Script().processStatement(session,"f x y;");
    }

    /**
     * Should throw an exception because z is not defined
     */
    @Test(expected = ScriptParseException.class)
    public void testBadReferenceScript() {
        SessionInput sessionInput = new Script().processStatement(session,"b = g x z;");
    }

    /**
     * Should throw an exception because f requires exactly 2 arguments
     */
    @Test(expected = ScriptParseException.class)
    public void testInvalidArguments() {
        SessionInput sessionInput = new Script().processStatement(session,"b = f x;");
    }

    @Test
    public void testParseNestedArgs() {
        SessionInput sessionInput = new Script()
                .processStatement(session, "a = h (text);");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 1;
        assert "text".equals(sessionInput.getArgs().get(0));
    }

    @Test
    public void testParseSingleNestedExpression() {
        // add the expected dependencies to working memory
        session.addStep(new Script().processStatement(session, "b = h text;"), new TestFrame(), false);

        SessionInput sessionInput = new Script()
                .processStatement(session, "a = h (h (text));");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 1;
        assert "h (text)".equals(sessionInput.getArgs().get(0));
    }

    @Test
    public void testParseMultipleNestedExpression() {
        // add the expected dependencies to working memory
        session.addStep(new Script().processStatement(session, "b = h text;"), new TestFrame(), false);
        session.addStep(new Script().processStatement(session, "c = f text text;"), new TestFrame(), false);

        SessionInput sessionInput = new Script()
                .processStatement(session, "a = h (h (text), f (text, text));");

        assert sessionInput.getAlias().isPresent();
        assert "a".equals(sessionInput.getAlias().get());
        assert sessionInput.getArgs().size() == 2;
        assert "h (text)".equals(sessionInput.getArgs().get(0));
        assert "f (text, text)".equals(sessionInput.getArgs().get(1));
    }

}
