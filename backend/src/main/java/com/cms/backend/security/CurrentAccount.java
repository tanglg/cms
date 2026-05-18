package com.cms.backend.security;

import com.cms.backend.account.UserAccount;
import java.security.Principal;

public record CurrentAccount(UserAccount account) implements Principal {
    @Override
    public String getName() {
        return account.getId();
    }
}
