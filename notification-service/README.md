# notification-service

Microservice Quarkus para notificacoes, alertas e comunicacao com usuarios.

## Funcionalidades

- E-mail automatico de fechamento mensal com resumo.
- Alerta quando pagamento do Mercado Livre esta proximo de liberar.
- Notificacao de nova venda opcional, ativavel pelo usuario.
- Relatorio semanal automatico enviado ao contador.
- Preferencias por tenant para habilitar/desabilitar notificacoes.

## Principais libs

- `quarkus-rest-jackson`: API REST JSON.
- `quarkus-mailer`: envio de e-mails.
- `quarkus-qute`: template HTML dos e-mails.
- `quarkus-scheduler`: jobs recorrentes.
- `quarkus-rest-client-jackson`: base para consultar outros microservices quando os contratos estiverem disponiveis.
- `quarkus-jdbc-postgresql` e `quarkus-flyway`: persistencia e migrations.
- `quarkus-messaging-kafka` e `quarkus-kafka-streams`: consumo de eventos, KStream e KTable materializada.

## Endpoints

| Metodo | Rota | Uso |
| --- | --- | --- |
| `GET` | `/notifications` | Status do servico |
| `GET` | `/notifications/tenants/{tenantId}/preferences` | Consultar preferencias |
| `PUT` | `/notifications/tenants/{tenantId}/preferences` | Atualizar preferencias |
| `GET` | `/notifications/tenants/{tenantId}` | Listar notificacoes |
| `GET` | `/notifications/tenants/{tenantId}/new-sale-summary` | Resumo materializado via Kafka Streams/KTable |
| `PATCH` | `/notifications/tenants/{tenantId}/{notificationId}/read` | Marcar como lida |
| `POST` | `/notifications/tenants/{tenantId}/clear-read` | Arquivar lidas |
| `POST` | `/notifications/events/new-sale` | Evento de nova venda |
| `POST` | `/notifications/events/ml-payment-release` | Evento de pagamento ML proximo de liberar |
| `POST` | `/notifications/events/monthly-closing` | Envio de fechamento mensal |
| `POST` | `/notifications/events/weekly-accountant-report` | Relatorio semanal ao contador |

## Controle de acesso

Os endpoints `/notifications/tenants/{tenantId}/...` exigem Bearer JWT emitido pelo `auth-service`. O `tenantId` do path deve bater com o claim `tenant_id` do token.

- `ADMIN` e `VENDEDOR`: podem consultar e alterar preferencias, marcar notificacoes como lidas e arquivar lidas.
- `CONTADOR`: pode consultar dados do proprio tenant, mas nao pode executar acoes de escrita.
- `/notifications/events/**`: aceita apenas `X-Internal-Token` para chamadas service-to-service e fica bloqueado no gateway publico.

## Kafka

O servico tambem consome eventos Kafka de nova venda pelo topico `brasaller.notifications.new-sale.v1`, com grupo `notification-service`. Mensagens que falharem no processamento sao encaminhadas para `brasaller.notifications.new-sale.dlq.v1`.

Kafka Streams fica habilitado no mesmo servico para analytics em tempo real:

- KStream de entrada: `brasaller.notifications.new-sale.v1`.
- KTable materializada: `tenant-new-sale-summary-store`.
- Topico compactado de saida: `brasaller.analytics.tenant-new-sale-summary.v1`.
- Endpoint de consulta: `GET /notifications/tenants/{tenantId}/new-sale-summary`.

A KTable mantem `saleCount`, `grossRevenue`, ultimo pedido, ultimo evento e timestamp do ultimo evento por tenant.

## Configuracao de e-mail

```properties
NOTIFICATION_MAIL_FROM=no-reply@braseller.local
SMTP_HOST=localhost
SMTP_PORT=1025
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_MOCK=true
```

Em dev/test, o mailer fica mockado. Em producao, configure SMTP real e `SMTP_MOCK=false`.

## Jobs

```properties
NOTIFICATION_MONTHLY_CLOSING_CRON=0 0 8 1 * ?
NOTIFICATION_ML_PAYMENT_RELEASE_CRON=0 0/30 * * * ?
NOTIFICATION_WEEKLY_ACCOUNTANT_REPORT_CRON=0 0 8 ? * MON
```

Os jobs estao preparados como pontos de orquestracao. Enquanto os contratos de vendas, Mercado Livre e contabilidade nao existem no repo, os disparos reais entram pelos endpoints de eventos.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

Swagger:

```text
http://localhost:8083/q/swagger-ui
```

## Testes

```shell
./mvnw test
```
