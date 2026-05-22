package com.example.application.port.out;

import com.example.domain.model.AuthIdentity;
import com.example.domain.model.IssuedTokens;

public interface TokenIssuer {
    IssuedTokens issue(AuthIdentity identity);
}
