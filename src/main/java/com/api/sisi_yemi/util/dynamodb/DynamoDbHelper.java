package com.api.sisi_yemi.util.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class DynamoDbHelper<T> {

    private final DynamoDbTable<T> table;

    public DynamoDbHelper(DynamoDbClient dynamoDbClient, String tableName, Class<T> clazz) {
        DynamoDbEnhancedClient enhancedClient = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(clazz));
    }

    public void save(T item) {
        table.putItem(item);
    }

    public Optional<T> getById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        return Optional.ofNullable(table.getItem(r -> r.key(key)));
    }

    public void deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        table.deleteItem(r -> r.key(key));
    }

    public List<T> findAll() {
        return table.scan().items().stream().toList();
    }

    public Optional<T> updateById(String id, Consumer<T> updater) {
        Optional<T> existingOpt = getById(id);
        existingOpt.ifPresent(updater.andThen(this::save));
        return existingOpt;
    }

    public List<T> queryByGsi(String indexName, String attributeName, String value) {
        AttributeValue attrValue = AttributeValue.builder().s(value).build();

        Expression expression = Expression.builder()
                .expression("#attr = :val")
                .expressionNames(Map.of("#attr", attributeName))
                .expressionValues(Map.of(":val", attrValue))
                .build();

        return table.index(indexName)
                .query(r -> r
                        .queryConditional(QueryConditional.keyEqualTo(Key.builder().partitionValue(value).build()))
                        .filterExpression(expression)
                )
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }
}

