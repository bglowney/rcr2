package com.rcr2.impl;

import com.rcr2.*;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.Scanner;

import static java.lang.System.out;

public abstract class CliSession<F extends DisplayableFrame<F>, C extends Context<F>> extends Session<F,C> {

    private static final String QUIT_CMD = ":quit";
    private static final String HELP_CMD = ":help";
    private static final String DISPLAY_CMD = ":display";
    private static final String DEBUG_CMD = ":debug";
    private static final String MEMORY_CMD = ":memory";
    private static final String IMITATE_CMD = ":imitate";
    private Scanner scanner;

    public void start() {
        out.print("\033[H\033[2J");
        out.flush();
        out.println("\nStarted session.\n");
        while (true) {
            String input = scanner.nextLine();
            // clear screen with ANSI escape code and place cursor at home
            out.print("\033[H\033[2J");
            out.flush();
            String[] words = input.split("\\s+");
            switch (words[0]) {
                case IMITATE_CMD:
                    beforeStep();
                    imitatorStep();
                    afterStep();
                    break;
                case DISPLAY_CMD:
                    if (words.length < 2)
                        out.println(":display requires an argument");
                    else {
                        String arg = words[1];
                        DisplayableFrame<F> frame = workingMemory.getFrame(arg);
                        if (frame == null)
                            out.println(String.format("Unrecognized argument %s", arg));
                        else
                            out.println(frame.display());
                    }
                    break;
                case MEMORY_CMD:
                    out.println(workingMemory.display());
                    break;
                case DEBUG_CMD:
                    out.println(displayDebug());
                    break;
                case HELP_CMD:
                    out.println(displayHelp());
                    break;
                case QUIT_CMD:
                    out.println("\nBye bye\n");
                    System.exit(0);
                    break;
                default:
                    try {
                        beforeStep();
                        imitatedStep(input)
                        .ifPresent(result -> out.println(result.display()));
                        afterStep();
                    } catch (ScriptParseException e) {
                        out.println(e.getMessage());
                    }
                    break;
            }
        }
    }

    protected abstract void beforeStep();

    protected abstract void afterStep();

    protected abstract String displayDebug();

    protected String displayHelp() {
        return "Available functions:\n\n" +
                context.displayFunctions() +
                "\n\nAvailable special commands:\n" +
                "\t" + IMITATE_CMD + "\tMake best guess function\n" +
                "\t" + MEMORY_CMD + "\tDisplay current memory\n" +
                "\t" + DISPLAY_CMD + " <var>\tDisplay a named variable\n" +
                "\t" + DEBUG_CMD + "\tDisplay debugging information\n" +
                "\t" + QUIT_CMD + "\tExit this session\n" +
                "\t" + HELP_CMD + "\tPrint this message\n";
    }

    public CliSession(@NotNull Persistence<F> persistence, @NotNull C context, @NotNull F initFrame) {
        this.persistence = persistence;
        this.context = context;
        this.workingMemory = new WorkingMemory<>(context, initFrame);
        this.currentFrame = initFrame;
        scanner = new Scanner(System.in);

        this.feedback = new Feedback<F>() {
            @Override
            public int score(F previous, F currentFrame) {
                out.println("Side effect successful. Getting feedback.");
                out.println("Previous: \n" + previous.display());
                out.println("Current: \n" + currentFrame.display());
                out.println("Enter score?");
                String scoreString = null;
                Integer score = null;
                while (score == null) {
                    scoreString = scanner.nextLine();
                    try {
                        score = Integer.parseInt(scoreString);
                    } catch (NumberFormatException e) {
                        out.println("Please enter an integer");
                    }
                }
                return score;
            }

            @Override
            public int failed() {
                out.println("Side effect failed. Getting feedback.");
                String scoreString = null;
                Integer score = null;
                while (score == null) {
                    scoreString = scanner.nextLine();
                    try {
                        score = Integer.parseInt(scoreString);
                    } catch (NumberFormatException e) {
                        out.println("Please enter an integer");
                    }
                }
                return score;
            }
        };

    }
}
