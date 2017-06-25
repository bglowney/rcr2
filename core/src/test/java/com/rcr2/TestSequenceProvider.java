package com.rcr2;

import com.rcr2.impl.InMemorySequenceProvider;

import java.util.ArrayList;

public class TestSequenceProvider extends InMemorySequenceProvider<TestFrame,TestContext> {
    public TestSequenceProvider() {
        this.addSequence(
            new InMemorySequenceProvider.Sequence(
                "seq1",
                new ArrayList<String>() {{
                    this.add("a = f text");
                    this.add("b = f a");
                    this.add("c = f b");
                }},
                new ArrayList<String>() {{
                    this.add("a");
                }}
            )
        ).addSequence(
           new InMemorySequenceProvider.Sequence(
        "seq2",
               new ArrayList<String>() {{
                   this.add("a = seq1");
                   this.add("b = f a");
                   this.add("c = f b");
               }},
               new ArrayList<String>() {{
                   this.add("a");
                   this.add("c");
               }}
            )
        );
    }
}
