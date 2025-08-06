package com.api.sisi_yemi.repository;

import com.api.sisi_yemi.model.Conversation;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationDynamoDbRepository {
    List<Conversation> findByUser(String userId);
    Optional<Conversation> findByParticipantAndUserAd(String participantId, String userAdId);
    Conversation save(Conversation conversation);

    Optional<Conversation> findById(String id);
}
