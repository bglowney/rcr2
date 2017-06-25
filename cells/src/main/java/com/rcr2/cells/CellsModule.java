package com.rcr2.cells;

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.dynamodbv2.util.TableUtils;
import com.google.inject.*;
import com.rcr2.FrameProvider;
import com.rcr2.Persistence;
import com.rcr2.SequenceProvider;
import com.rcr2.impl.DynamoDBPersistence;
import com.rcr2.impl.InMemorySequenceProvider;
import lombok.val;

import static com.rcr2.impl.DynamoDBPersistence.FEEDBACK_STATS_TABLE;
import static com.rcr2.impl.DynamoDBPersistence.PRIOR_STATEMENT_KEY;
import static com.rcr2.impl.DynamoDBPersistence.SUBSEQUENT_STATEMENT_KEY;

public class CellsModule extends AbstractModule {

    public static void main(String[] args) {
        val injector = Guice.createInjector(new CellsModule());
        val session = injector.getInstance(CellSession.class);
        session.start();
    }

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
    SequenceProvider<CellFrame,CellsContext> sequenceProvider() {
        return new InMemorySequenceProvider<>();
    }

    @Provides
    @Singleton
    CellFrame.Player mainCell() {
        return new CellFrame.Player(5,5);
    }

    @Provides
    @Singleton
    @Inject
    CellsContext context(SequenceProvider<CellFrame,CellsContext> sequenceProvider, CellFrame.Player mainCell) {
        val context = new CellsContext(
            sequenceProvider,
            mainCell,
            CellsContext.CELLS_LENGTH,
            CellsContext.CELLS_WIDTH
        );

        Functions.setContextFunctions(context);

        return context;
    }

    @Provides
    @Singleton
    @Inject
    FrameProvider<CellFrame> frameProvider(CellFrame.Player mainCell) {
        return () -> new CellFrame(mainCell);
    }

    @Provides
    @Singleton
    @Inject
    Persistence<CellFrame,CellsContext> persistence(AmazonDynamoDB dynamoDB) {
        return new DynamoDBPersistence<>(new DynamoDBMapper(dynamoDB));
    }

    @Provides
    @Singleton
    @Inject
    CellSession context(Persistence<CellFrame,CellsContext> persistence, CellsContext cellsContext, FrameProvider<CellFrame> frameProvider) {
        return new CellSession(persistence, cellsContext, frameProvider);
    }
}
