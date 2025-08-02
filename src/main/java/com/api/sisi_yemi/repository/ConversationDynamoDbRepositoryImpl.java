package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.util.DynamoDbUtilHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ConversationDynamoDbRepositoryImpl implements ConversationDynamoDbRepository {

    private final DynamoDbUtilHelper dynamoDbUtilHelper;

    private DynamoDbTable<Conversation> table() {
        return dynamoDbUtilHelper.getConversationTable().getRawTable();
    }

    @Override
    public List<Conversation> findByUser(String userId) {
        Set<String> seenIds = new HashSet<>();
        List<Conversation> allConversations = new ArrayList<>();

        // Query conversations where the user is a participant
        try {
            DynamoDbIndex<Conversation> participantIndex = table().index("participantId-index");
            QueryConditional participantCondition = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(userId).build()
            );

            List<Conversation> participantConversations = participantIndex.query(r -> r.queryConditional(participantCondition))
                    .stream()
                    .flatMap(p -> p.items().stream())
                    .toList();

            for (Conversation c : participantConversations) {
                if (seenIds.add(c.getId())) {
                    allConversations.add(c);
                }
            }
        } catch (Exception e) {
            // Optional: Log error
            log.warn("Failed to query participantId-index for user {}: {}", userId, e.getMessage());
        }

        // Query conversations where the user is a seller
        try {
            DynamoDbIndex<Conversation> sellerIndex = table().index("sellerId-index");
            QueryConditional sellerCondition = QueryConditional.keyEqualTo(
                    Key.builder().partitionValue(userId).build()
            );

            List<Conversation> sellerConversations = sellerIndex.query(r -> r.queryConditional(sellerCondition))
                    .stream()
                    .flatMap(p -> p.items().stream())
                    .toList();

            for (Conversation c : sellerConversations) {
                if (seenIds.add(c.getId())) {
                    allConversations.add(c);
                }
            }
        } catch (Exception e) {
            // Optional: Log error
            log.warn("Failed to query sellerId-index for user {}: {}", userId, e.getMessage());
        }

        return allConversations;
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
