package com.example.application.port.out;

import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.AccountantClientView;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.TenantCompanyProfile;
import com.example.domain.model.UserView;

import java.util.List;
import java.util.Optional;

public interface UserIdentityRepository {

    RegisteredTenant registerTenant(String legalName, String tradeName, String adminName,
                                    String email, String passwordHash, TenantCompanyProfile companyProfile);

    /**
     * Persiste o usuário contador e o registro de acesso.
     *
     * @param accountantUserId  ID pré-gerado pelo service (UUID)
     * @param tenantId          Tenant que concede o acesso
     * @param accountantEmail   E-mail do contador
     * @param accountantFullName Nome completo (firstName + " " + lastName)
     * @param firstName         Primeiro nome (para coluna first_name)
     * @param lastName          Sobrenome (para coluna last_name)
     * @param passwordHash      Hash da senha temporária (ou "KEYCLOAK_MANAGED" para usuários Keycloak)
     * @param provider          "KEYCLOAK" quando criado via Admin API, "PASSWORD" caso contrário
     * @param providerSubject   Keycloak user ID (sub), null para usuários locais
     * @param status            "ACTIVE" (Keycloak) ou "INVITED" (local)
     * @param grantedByUserId   ID do usuário que concedeu o acesso
     */
    AccountantAccessView grantAccountantAccess(
            String accountantUserId,
            String tenantId,
            String accountantEmail,
            String accountantFullName,
            String firstName,
            String lastName,
            String passwordHash,
            String provider,
            String providerSubject,
            String status,
            String grantedByUserId
    );

    List<UserView> listTenantUsers(String tenantId);

    List<AccountantClientView> listAccountantClients(String userId, String email);

    List<AccountantClientView> listAllBpoClients();

    List<String> listAccountantTenantIds(String userId, String email);

    Optional<UserView> findUserByEmail(String email);

    Optional<StoredUserCredentials> findActiveCredentialsByEmail(String email);

    Optional<UserView> syncExternalProfile(String email, String provider, String providerSubject, String fullName,
                                           String preferredUsername, String firstName, String lastName,
                                           String pictureUrl, boolean emailVerified);
}
