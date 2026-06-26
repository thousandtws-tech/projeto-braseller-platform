# Integração Open Finance com Pluggy Connect

Este projeto integra o fluxo de consentimento Open Finance usando o `Pluggy Connect` no frontend e criação de `Connect Token` no `core-service`.

## Segurança de credenciais

As credenciais da Pluggy devem ficar somente no ambiente do backend. Não coloque `PLUGGY_CLIENT_ID`, `PLUGGY_CLIENT_SECRET` ou `API Key` em código frontend, arquivos públicos ou variáveis `NEXT_PUBLIC_*`.

Configure no ambiente do `core-service`:

```env
PLUGGY_CLIENT_ID=replace-with-real-client-id
PLUGGY_CLIENT_SECRET=replace-with-real-client-secret
PLUGGY_API_BASE_URL=https://api.pluggy.ai
```

No frontend/Next.js, configure apenas opções não sensíveis:

```env
PLUGGY_CONNECT_INCLUDE_SANDBOX=true
```

O usuário responsável pelo deploy deve substituir os placeholders por acessos reais no `process.env`, Azure Container Apps ou secret store equivalente.

## Endpoints

### Criar Connect Token

```http
POST /api/core/connectors/open-finance/pluggy/connect-token
Authorization: Bearer <jwt>
Content-Type: application/json

{}
```

Resposta:

```json
{
  "accessToken": "pluggy-connect-token"
}
```

### Webhook Pluggy

```http
POST /api/core/connectors/open-finance/pluggy/webhooks
Content-Type: application/json
```

Este endpoint confirma recebimento com `2XX` rapidamente. Processamentos pesados de eventos como `item/created`, `item/updated` e `item/error` devem ser movidos para fila/job assíncrono quando a sincronização bancária for evoluída.

## Frontend

A tela `Conectores` mostra a opção `Open Finance`, gera o `Connect Token` pelo backend e abre o widget `react-pluggy-connect`. O `clientId` e o `clientSecret` nunca são enviados ao navegador.
