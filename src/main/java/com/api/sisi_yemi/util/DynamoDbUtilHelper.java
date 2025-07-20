package com.api.sisi_yemi.util;

import com.api.sisi_yemi.model.Conversation;
import com.api.sisi_yemi.model.Message;
import com.api.sisi_yemi.model.User;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelper;
import com.api.sisi_yemi.util.dynamodb.DynamoDbHelperFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DynamoDbUtilHelper {

    private final DynamoDbHelperFactory factory;

    public DynamoDbHelper<Message> getMessageTable() {
        return factory.getHelper("messages", Message.class);
    }

    public DynamoDbHelper<Conversation> getConversationTable() {
        return factory.getHelper("conversations", Conversation.class);
    }

    public DynamoDbHelper<User> getUserTable() {
        return factory.getHelper("users", User.class);
    }
}

