package com.rcr2.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import com.rcr2.Context;
import com.rcr2.Frame;
import com.rcr2.Persistence;
import lombok.*;

import java.util.Collection;

@AllArgsConstructor
public class DynamoDBPersistence<F extends Frame<F>, C extends Context<F,C>> extends AbstractPersistence<F,C> {

    final DynamoDBMapper dynamoDB;

    public final static String FEEDBACK_STATS_TABLE = "FeedbackStats";
    public final static String PRIOR_STATEMENT_KEY = "priorStatement";
    public final static String SUBSEQUENT_STATEMENT_KEY = "subsequentStatement";

    @DynamoDBTable(tableName = FEEDBACK_STATS_TABLE)
    @NoArgsConstructor
    public static class FeedbackStats implements Persistence.FeedbackStats {

        public FeedbackStats(String priorStatement, String subsequentStatement) {
            this.priorStatement = priorStatement;
            this.subsequentStatement = subsequentStatement;
        }
        String priorStatement;
        String subsequentStatement;
        Integer count;
        Integer cumulative;

        @Override
        @DynamoDBHashKey(attributeName = PRIOR_STATEMENT_KEY)
        public String getPriorStatement() {
            return priorStatement;
        }

        public void setPriorStatement(String priorStatement) {
            this.priorStatement = priorStatement;
        }

        @Override
        @DynamoDBRangeKey(attributeName = SUBSEQUENT_STATEMENT_KEY)
        public String getSubsequentStatement() {
            return subsequentStatement;
        }

        public void setSubsequentStatement(String subsequentStatement) {
            this.subsequentStatement = subsequentStatement;
        }

        @Override
        @DynamoDBAttribute(attributeName = "count")
        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        @DynamoDBAttribute(attributeName = "cumulative")
        public Integer getCumulative() {
            return cumulative;
        }

        public void setCumulative(Integer cumulative) {
            this.cumulative = cumulative;
        }

        @DynamoDBIgnore
        public double getExpectedValue() {
            return (double)cumulative / (double)count;
        }

        public void increment(int value) {
            if (count == null)
                count = 0;
            if (cumulative == null)
                cumulative = 0;
            this.count++;
            this.cumulative += value;
        }

    }

    public Collection<? extends Persistence.FeedbackStats> getFeedbackStats(String currentState) {
        val expression = new DynamoDBScanExpression()
                .addExpressionAttributeNamesEntry(PRIOR_STATEMENT_KEY, currentState);
        return dynamoDB.scan(FeedbackStats.class, expression);
    }

    protected void addObservation(String previous, String next, int score) {
        val newStats = new FeedbackStats(previous, next);
        val oldStats = dynamoDB.load(newStats);
        val toUpdate = oldStats == null ? newStats : oldStats;
        toUpdate.increment(score);
        dynamoDB.save(toUpdate);
    }
}
