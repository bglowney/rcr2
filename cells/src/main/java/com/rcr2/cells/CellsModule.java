package com.rcr2.cells;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.rcr2.Persistence;
import com.rcr2.impl.DynamoDBPersistence;
import lombok.val;

import static com.rcr2.impl.DynamoDBPersistence.FEEDBACK_STATS_TABLE;
import static com.rcr2.impl.DynamoDBPersistence.PRIOR_STATEMENT_KEY;
import static com.rcr2.impl.DynamoDBPersistence.SUBSEQUENT_STATEMENT_KEY;

public class CellsModule extends AbstractModule {

    @Override
    protected void configure() {

    }

    @Provides
    @Singleton
    AmazonDynamoDB client() {
        val dynamoDB = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(
                    new EndpointConfiguration(
                            "http://localhost:8000",
                            "us-west-2")
                ).build();

        TableUtils.createTableIfNotExists(dynamoDB,
                new CreateTableRequest()
                    .withTableName(FEEDBACK_STATS_TABLE)
                    .withKeySchema(new KeySchemaElement(PRIOR_STATEMENT_KEY, KeyType.HASH))
                    .withKeySchema(new KeySchemaElement(SUBSEQUENT_STATEMENT_KEY, KeyType.RANGE))
                    .withAttributeDefinitions(new AttributeDefinition(PRIOR_STATEMENT_KEY, ScalarAttributeType.S))
                    .withAttributeDefinitions(new AttributeDefinition(SUBSEQUENT_STATEMENT_KEY, ScalarAttributeType.S))
                    .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L)));

        return dynamoDB;
    }

    @Provides
    @Singleton
    @Inject
    Persistence<CellFrame> persistence(AmazonDynamoDB dynamoDB) {
        return new DynamoDBPersistence<>(new DynamoDBMapper(dynamoDB));
    }

    @Provides
    @Singleton
    @Inject
    CellSession session(Persistence<CellFrame> persistence) {
        return new CellSession(persistence);
    }
}
