package com.api.sisi_yemi.util.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.List;
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

    public DynamoDbTable<T> getRawTable() {
        return table;
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

    public List<T> queryByGsi(String indexName, String partitionKeyName, String partitionKeyValue) {
        return table.index(indexName)
                .query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(Key.builder().partitionValue(partitionKeyValue).build())
                ))
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }

    public List<T> queryByCompositeGsi(String indexName, String userId, String adId) {
        return table.index(indexName)
                .query(r -> r.queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(userId).sortValue(adId).build()
                )))
                .stream()
                .flatMap(page -> page.items().stream())
                .toList();
    }
}

