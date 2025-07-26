package com.api.sisi_yemi.util.auth;

import java.security.Principal;
import java.util.List;

public record UserPrincipal(String userId, String email, List<String> roles) implements Principal {
    @Override
    public String getName() {
        return userId;
    }

}
