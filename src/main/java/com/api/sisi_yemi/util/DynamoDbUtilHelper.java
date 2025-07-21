package com.api.sisi_yemi.util;

import com.api.sisi_yemi.model.*;
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

    public DynamoDbHelper<Favorite> getFavoriteTable() {
        return factory.getHelper("favorites", Favorite.class);
    }

    public DynamoDbHelper<UserAd> getUserAdsTable() {
        return factory.getHelper("user_ads", UserAd.class);
    }
}

