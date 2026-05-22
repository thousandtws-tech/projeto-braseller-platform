package com.example.application.port.out;

import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.UserView;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository {
    RegisteredTenant registerTenant(String legalName, String tradeName, String adminName, String email, String passwordHash);

    AccountantAccessView grantAccountantAccess(String tenantId, String accountantEmail, String accountantName,
                                               String temporaryPasswordHash, String grantedByUserId);

    List<UserView> listTenantUsers(String tenantId);

    Optional<StoredUserCredentials> findActiveCredentialsByEmail(String email);
}
