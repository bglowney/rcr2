package com.rcr2.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.rcr2.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;

import java.util.List;

@RequiredArgsConstructor
public class DynamoDBSequenceSupplier<F extends Frame<F>, C extends Context<F,C>> implements SequenceProvider<F,C> {

    final DynamoDBMapper dynamoDB;

    private static class Sequence {
        @DynamoDBHashKey @Getter @Setter
        String name;
        @Getter @Setter
        List<String> scripts;
        @Getter @Setter
        List<String> components;
    }

    @Override
    public Function.Sequence<F,C> forName(String name, Session<F,C> session, F baseFrame) {
        val sequence = dynamoDB.load(Sequence.class, name);
        if (sequence == null)
            return null;
        return new Function.Sequence<>(session, baseFrame, sequence.getScripts(), sequence.getComponents());
    }

    @Override
    public boolean hasSequence(String name) {
        return dynamoDB.load(Sequence.class, name) != null;
    }
}
