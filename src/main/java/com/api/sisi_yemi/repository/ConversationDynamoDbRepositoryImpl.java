package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.Conversation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Repository
public class ConversationDynamoDbRepositoryImpl implements ConversationDynamoDbRepository {

    private final DynamoDbEnhancedClient enhancedClient;

    private DynamoDbTable<Conversation> table() {
        return enhancedClient.table("conversations", TableSchema.fromBean(Conversation.class));
    }

    @Override
    public List<Conversation> findByUser(String userId) {
        DynamoDbIndex<Conversation> index = table().index("participantId-index");

        QueryConditional condition = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());

        return index.query(r -> r.queryConditional(condition))
                .stream()
                .flatMap(p -> p.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Conversation> findByParticipantAndUserAd(String participantId, String userAdId) {
        DynamoDbIndex<Conversation> index = table().index("participantId-userAdId-index");

        QueryConditional condition = QueryConditional.keyEqualTo(Key.builder().partitionValue(participantId).sortValue(userAdId).build());

        return index.query(r -> r.queryConditional(condition))
                .stream()
                .flatMap(p -> p.items().stream())
                .findFirst();
    }

    @Override
    public Conversation save(Conversation conversation) {
        if (conversation.getId() == null) {
            conversation.setId(UUID.randomUUID().toString());
        }
        table().putItem(conversation);
        return conversation;
    }

}
