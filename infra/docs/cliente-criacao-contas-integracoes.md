# Documento para Criacao de Contas e Integracoes

Projeto: BraSeller Sistema Modular

Finalidade: orientar o cliente na criacao das contas externas necessarias para desenvolvimento, homologacao e producao do sistema, incluindo repositorio de codigo, containers, infraestrutura cloud, APIs de marketplace, assinatura digital, pagamentos, comunicacao e autenticacao.

## 1. Orientacoes Gerais

As contas devem ser criadas em nome da empresa contratante, usando e-mail corporativo e dados juridicos oficiais. A equipe tecnica deve receber acesso por convite, token restrito ou credencial tecnica, nunca por compartilhamento de senha pessoal.

Regras obrigatorias:

- Usar e-mail corporativo para contas administrativas.
- Ativar MFA/2FA em todas as plataformas que oferecerem esse recurso.
- Criar ambientes separados sempre que possivel: `sandbox`, `homologacao` e `producao`.
- Nao enviar senhas por WhatsApp, e-mail aberto ou print.
- Preferir convite de usuario, permissao por papel ou token tecnico com escopo minimo.
- Registrar quem e o proprietario financeiro/administrativo de cada conta.
- Habilitar alertas de custo em ferramentas pagas, principalmente Azure, Docker, Twilio e provedor de pagamento.
- Guardar secrets em cofre seguro, como Azure Key Vault, 1Password, Bitwarden, Doppler ou equivalente.

## 2. Responsabilidades

| Responsavel | Responsabilidade |
| --- | --- |
| Cliente | Criar contas juridicas, aprovar termos, cadastrar meio de pagamento, validar documentos, liberar acessos e autorizar integracoes. |
| Equipe tecnica | Orientar configuracoes, receber credenciais tecnicas, configurar variaveis de ambiente, testar integracoes e documentar uso. |
| Contador/Financeiro | Validar dados fiscais, regime tributario, documentacao de pagamento, notas fiscais e regras contabeis. |

## 3. Checklist Executivo

| Item | Ferramenta | Uso no projeto | Obrigatorio no MVP | Responsavel |
| --- | --- | --- | --- | --- |
| 1 | GitHub | Repositorio de codigo, versionamento e CI/CD | Sim | Cliente + Equipe tecnica |
| 2 | Docker | Imagens, containers e ambiente local/producao | Sim | Cliente + Equipe tecnica |
| 3 | Cloudinary | Midias, imagens, documentos e arquivos de apoio | Recomendado | Cliente |
| 4 | Azure | Infraestrutura, banco, containers, secrets e observabilidade | Sim para producao | Cliente + Equipe tecnica |
| 5 | Clicksign | Assinatura digital de contratos/documentos | Conforme escopo | Cliente |
| 6 | Mercado Livre | Pedidos, pagamentos, taxas e dados do vendedor | Sim no Modulo 1 | Cliente |
| 7 | Shopee | Pedidos, pagamentos e dados da loja | Fase seguinte | Cliente |
| 8 | Amazon | Pedidos e dados via SP-API | Fase seguinte | Cliente |
| 9 | Pagamentos | Planos, assinaturas e cobranca do SaaS | A definir | Cliente |
| 10 | Twilio | SMS, WhatsApp, OTP e comunicacao transacional | Conforme escopo | Cliente |

## 4. GitHub

Uso no projeto:

- Hospedar codigo-fonte.
- Controlar versoes.
- Organizar branches, pull requests e releases.
- Configurar CI/CD.
- Armazenar secrets de pipeline quando necessario.

O cliente deve criar:

1. Conta GitHub usando e-mail corporativo.
2. Organizacao GitHub da empresa.
3. Repositorio privado para o projeto, caso ainda nao exista.
4. Times de acesso, por exemplo:
   - `owners`: administradores do cliente.
   - `developers`: equipe de desenvolvimento.
   - `read-only`: auditoria/contabilidade, se aplicavel.
5. MFA/2FA obrigatorio para administradores.

Dados a entregar para a equipe tecnica:

| Dado | Observacao |
| --- | --- |
| Nome da organizacao | Exemplo: `empresa-braseller`. |
| URL do repositorio | Repositorio privado do projeto. |
| Usuario/e-mail dos responsaveis | Para convites e aprovacoes. |
| Politica de acesso | Quem aprova merge, releases e deploys. |

Links oficiais:

- Criar conta GitHub: https://docs.github.com/en/get-started/start-your-journey/creating-an-account-on-github
- Criar organizacao: https://docs.github.com/en/organizations/collaborating-with-groups-in-organizations/creating-a-new-organization-from-scratch

## 5. Docker

Uso no projeto:

- Criar e executar containers dos microservicos.
- Publicar imagens Docker.
- Apoiar ambiente local, homologacao e producao.

O cliente deve criar:

1. Conta Docker.
2. Organizacao Docker Hub, se o projeto for publicar imagens privadas no Docker Hub.
3. Times de acesso para administradores e equipe tecnica.
4. Token de acesso para CI/CD, preferencialmente com permissao limitada.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Docker ID | Usuario principal ou organizacao. |
| Namespace/organizacao | Onde as imagens ficarao publicadas. |
| Token tecnico | Somente se o pipeline for publicar imagens no Docker Hub. |
| Politica de repositorios | Publico ou privado; recomendacao: privado. |

Links oficiais:

- Criar conta Docker: https://docs.docker.com/accounts/create-account/
- Criar organizacao Docker: https://docs.docker.com/admin/organization/orgs/
- Tokens de acesso: https://docs.docker.com/security/for-admins/access-tokens/

## 6. Cloudinary

Uso no projeto:

- Armazenamento e entrega de imagens, documentos e arquivos auxiliares.
- Upload seguro de midias vinculadas a usuarios, produtos, anexos fiscais ou documentos.

O cliente deve criar:

1. Conta Cloudinary.
2. Ambiente/produto para o projeto.
3. Configuracao de `cloud name`.
4. API Key e API Secret para integracao backend.
5. Upload presets quando houver upload direto do frontend.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| `CLOUDINARY_CLOUD_NAME` | Identificador publico do ambiente Cloudinary. |
| `CLOUDINARY_API_KEY` | Chave publica tecnica. |
| `CLOUDINARY_API_SECRET` | Secret; enviar somente por canal seguro. |
| Upload preset | Se existir upload direto do navegador. |

Links oficiais:

- Visao geral Cloudinary: https://cloudinary.com/documentation/solution_overview
- SDKs e credenciais: https://cloudinary.com/documentation/cloudinary_sdks

## 7. Azure

Uso no projeto:

- Hospedagem dos microservicos.
- Banco de dados PostgreSQL gerenciado.
- Registry de containers.
- Rede, monitoramento, logs e secrets.
- Autenticacao/identidade de infraestrutura.

O cliente deve criar:

1. Conta Microsoft/Azure em nome da empresa.
2. Subscription de producao.
3. Resource Group do projeto.
4. Budget e alertas de custo.
5. Acesso da equipe tecnica por convite com papel adequado.
6. Recursos recomendados:
   - Azure Container Registry ou registry equivalente.
   - Azure Container Apps, App Service ou AKS, conforme decisao de infraestrutura.
   - Azure Database for PostgreSQL Flexible Server.
   - Azure Key Vault para secrets.
   - Application Insights/Azure Monitor para logs e metricas.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Tenant ID | Identificacao do diretorio Azure. |
| Subscription ID | Assinatura onde os recursos serao criados. |
| Resource Group | Nome do grupo de recursos. |
| Permissao tecnica | Convite para equipe tecnica; evitar uso de conta compartilhada. |
| Politica de custo | Limites, alertas e responsavel financeiro. |

Links oficiais:

- Criar conta Azure: https://azure.microsoft.com/en-us/pricing/purchase-options/azure-account
- Criar subscription: https://learn.microsoft.com/en-us/azure/cost-management-billing/manage/create-subscription

## 8. Clicksign

Uso no projeto:

- Assinatura digital/eletronica de documentos.
- Envio de contratos.
- Webhooks de eventos de assinatura.
- Possivel uso de WhatsApp, SMS, e-mail e metodos de autenticacao adicionais.

O cliente deve criar:

1. Conta Clicksign.
2. Ambiente de teste/sandbox, se disponivel no plano contratado.
3. Usuario API.
4. Access token para API.
5. Configuracao de webhooks para ambiente de homologacao e producao.
6. Definicao dos metodos de autenticacao exigidos por tipo de documento.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Access token sandbox | Para testes. |
| Access token producao | Enviar somente por canal seguro. |
| URL/base API | Confirmar ambiente utilizado. |
| Webhook configurado | URL de eventos do sistema, quando definida. |
| Metodos de assinatura | E-mail, SMS, WhatsApp, documento oficial, biometria etc. |

Links oficiais:

- Site Clicksign: https://www.clicksign.com/
- Central API Clicksign: https://ajuda.clicksign.com/api?hsLang=pt-br
- Documentacao tecnica: https://developers.clicksign.com/

## 9. Mercado Livre

Uso no projeto:

- OAuth 2.0 para autorizar a conta do vendedor.
- Pedidos.
- Itens do pedido.
- Pagamentos.
- Taxas.
- Dados do vendedor.
- Datas normalizadas para `America/Sao_Paulo`.

O cliente deve criar/configurar:

1. Conta de vendedor Mercado Livre.
2. Conta no portal de desenvolvedores Mercado Livre.
3. Aplicacao no painel "Minhas aplicacoes".
4. Redirect URI HTTPS.
5. Fluxos OAuth:
   - Authorization Code.
   - Refresh Token.
6. Scopes/permissoes de leitura necessarias para pedidos, usuarios e pagamentos.
7. Unidade de negocio Mercado Livre/VIS, conforme exibido no painel.

Configuracao atual do projeto:

```env
MERCADOLIVRE_CLIENT_ID=
MERCADOLIVRE_CLIENT_SECRET=
MERCADOLIVRE_REDIRECT_URI=
MERCADOLIVRE_REFRESH_SKEW_SECONDS=300
```

Observacoes importantes:

- A `redirect_uri` deve ser exatamente igual no painel Mercado Livre, na URL de autorizacao e no backend.
- A URL de redirect precisa usar HTTPS.
- No MVP atual, PKCE deve permanecer desativado. Se o cliente exigir PKCE, a implementacao backend deve ser ajustada para enviar `code_challenge` e `code_verifier`.
- Tokens expiram e sao renovados automaticamente pelo backend com refresh token.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| App ID / Client ID | Identificacao da aplicacao. |
| Client Secret | Secret da aplicacao; enviar por canal seguro. |
| Redirect URI | URL HTTPS cadastrada no Mercado Livre. |
| Seller/User ID | Identificacao da conta vendedora, quando disponivel. |
| Ambiente | Homologacao ou producao. |

Links oficiais:

- Criar aplicacao Mercado Livre: https://developers.mercadolivre.com.br/en_us/javascript/register-your-application
- Autenticacao e autorizacao: https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao?nocache=true

## 10. Shopee

Uso previsto:

- Integracao futura com pedidos, pagamentos, produtos, estoque e status da loja.

O cliente deve criar/configurar:

1. Conta de vendedor Shopee.
2. Acesso ao Shopee Open Platform.
3. Cadastro como parceiro/desenvolvedor, quando aplicavel.
4. Aplicacao/API no painel da Shopee.
5. Credenciais tecnicas:
   - Partner ID.
   - Partner Key.
   - Shop ID.
6. URL de redirect/callback para autorizacao da loja.
7. Permissoes para pedidos, produtos, pagamentos e estoque, conforme escopo aprovado.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Partner ID | Identificador da aplicacao/parceiro. |
| Partner Key | Chave secreta; enviar por canal seguro. |
| Shop ID | Identificacao da loja. |
| Redirect URL | URL HTTPS de callback. |
| Ambiente | Sandbox/homologacao/producao, se disponivel. |

Links oficiais:

- Shopee Open Platform: https://open.shopee.com/
- Documentacao oficial: https://open.shopee.com/documents

## 11. Amazon

Uso previsto:

- Integracao futura com Amazon Selling Partner API para pedidos, envios, pagamentos e dados da conta vendedora.

O cliente deve criar/configurar:

1. Conta profissional de vendedor Amazon.
2. Acesso como usuario principal da conta vendedora.
3. Registro como desenvolvedor SP-API.
4. Aplicacao privada, caso o sistema seja usado apenas para a propria operacao do cliente.
5. Login with Amazon (LWA):
   - Client ID.
   - Client Secret.
6. Configuracao AWS/IAM exigida pela SP-API, quando aplicavel.
7. Seller ID e Marketplace ID.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Seller ID | Identificacao da conta vendedora. |
| Marketplace ID | Exemplo: Brasil, EUA etc. |
| LWA Client ID | Client ID da aplicacao. |
| LWA Client Secret | Secret; enviar por canal seguro. |
| Refresh token | Gerado apos autorizacao da aplicacao. |
| AWS Role ARN / credenciais IAM | Quando exigido pela configuracao SP-API. |

Observacoes:

- A Amazon pode exigir aprovacao do perfil de desenvolvedor.
- A conta deve ser profissional e o cadastro deve ser feito pelo usuario principal da conta vendedora.
- O prazo de liberacao pode variar conforme analise da Amazon.

Links oficiais:

- Amazon SP-API: https://sell.amazon.com/developers
- Registro SP-API: https://developer-docs.amazon.com/sp-api/docs/sp-api-registration-overview

## 12. Sistema de Pagamento para Planos

Decisao pendente com o cliente: escolher qual provedor sera usado para planos, assinaturas, upgrades, downgrades, cobranca recorrente e webhooks.

Opcoes comuns:

| Provedor | Quando considerar |
| --- | --- |
| Stripe | SaaS com assinatura, cartao, boleto em alguns mercados, checkout moderno e boa documentacao. |
| Pagar.me | Operacao brasileira, Pix, boleto, cartao, split/marketplace e conciliacao local. |
| Asaas | Cobranca recorrente no Brasil, Pix, boleto e gestao de recebiveis. |
| Iugu | Assinaturas, boleto, Pix e cobranca recorrente. |

O cliente deve decidir:

- Provedor principal.
- Moeda e pais de cobranca.
- Planos: Basic, Pro, Agency ou outros.
- Ciclo: mensal, anual ou ambos.
- Formas de pagamento: cartao, Pix, boleto.
- Politica de trial.
- Politica de inadimplencia e suspensao.
- URL de webhook de producao.

Dados a entregar apos escolha:

| Dado | Observacao |
| --- | --- |
| API Key / Secret Key | Secret; enviar por canal seguro. |
| Public Key | Quando houver uso no frontend. |
| Webhook secret | Para validacao de eventos. |
| Conta bancaria | Para repasse/recebimento. |
| Dados fiscais | CNPJ, razao social, endereco, responsavel legal. |
| Ambiente sandbox | Para testes antes da producao. |

Links oficiais de referencia:

- Stripe API keys: https://docs.stripe.com/keys
- Stripe primeiros passos: https://docs.stripe.com/development/get-started
- Pagar.me quickstart: https://docs.pagar.me/docs/quickstart-pagarme

## 13. Twilio

Uso previsto:

- Envio de SMS.
- WhatsApp transacional.
- OTP/verificacao.
- Comunicacao com usuarios e clientes.

O cliente deve criar/configurar:

1. Conta Twilio.
2. Projeto/Account para o sistema.
3. Upgrade da conta, se necessario para producao.
4. Numero telefonico, Messaging Service ou WhatsApp Sender, conforme escopo.
5. API Key tecnica.
6. Definicao dos paises e canais permitidos.
7. Limites de gasto e alertas.

Dados a entregar:

| Dado | Observacao |
| --- | --- |
| Account SID | Identificacao da conta Twilio. |
| API Key SID | Chave tecnica. |
| API Key Secret | Secret; enviar somente por canal seguro. |
| Messaging Service SID | Se usar SMS/WhatsApp via Messaging Service. |
| Verify Service SID | Se usar OTP/Verify. |
| Numero ou sender aprovado | Telefone, WhatsApp ou sender ID. |

Links oficiais:

- Twilio Docs: https://www.twilio.com/docs
- Twilio IAM/API Keys: https://www.twilio.com/docs/iam
- Ajuda Twilio API Keys: https://help.twilio.com/articles/9318455807771

## 14. Contas e Servicos Adicionais Recomendados

### Dominio e DNS

Necessario para producao e para redirects HTTPS de OAuth.

Exemplos:

- Registro.br.
- Cloudflare.
- Azure DNS.

Dados esperados:

- Dominio principal.
- Acesso ao painel DNS ou convite para equipe tecnica.
- Subdominios desejados:
  - `api.dominio.com.br`
  - `app.dominio.com.br`
  - `auth.dominio.com.br`
  - `admin.dominio.com.br`

### Sistema de Autenticacao

O projeto usa arquitetura com `auth-service` e Keycloak. Em producao, sera necessario definir se o Keycloak sera:

- Hospedado na propria infraestrutura Azure.
- Substituido por provedor gerenciado, como Azure AD B2C, Auth0, Clerk ou equivalente.

Dados esperados se mantiver Keycloak:

- URL publica do Keycloak.
- Realm de producao.
- Client ID e Client Secret.
- Politicas de senha.
- MFA, se exigido.
- Redirect URIs do frontend.

### Banco de Dados

Recomendacao para producao:

- PostgreSQL gerenciado, preferencialmente Azure Database for PostgreSQL Flexible Server.
- Backups automaticos.
- Acesso restrito por rede/firewall.
- Usuario tecnico por servico.
- Ambiente separado para homologacao e producao.

Dados esperados:

- Host.
- Porta.
- Nome do banco.
- Usuario.
- Senha.
- Politica de backup.
- Janela de manutencao.

### Observabilidade

O projeto possui Prometheus e Grafana no ambiente atual. Para producao, decidir entre:

- Grafana self-hosted.
- Grafana Cloud.
- Azure Monitor/Application Insights.

Dados esperados:

- Quem recebe alertas.
- Canais de alerta: e-mail, Slack, Teams, WhatsApp.
- Limites de CPU, memoria, erros HTTP, indisponibilidade e fila/eventos.

## 15. Modelo de Envio Seguro de Credenciais

O cliente deve preencher os dados abaixo apenas em ferramenta segura. Nao preencher em e-mail aberto.

| Categoria | Nome da variavel | Valor |
| --- | --- | --- |
| Mercado Livre | `MERCADOLIVRE_CLIENT_ID` |  |
| Mercado Livre | `MERCADOLIVRE_CLIENT_SECRET` |  |
| Mercado Livre | `MERCADOLIVRE_REDIRECT_URI` |  |
| Shopee | `SHOPEE_PARTNER_ID` |  |
| Shopee | `SHOPEE_PARTNER_KEY` |  |
| Amazon | `AMAZON_LWA_CLIENT_ID` |  |
| Amazon | `AMAZON_LWA_CLIENT_SECRET` |  |
| Cloudinary | `CLOUDINARY_CLOUD_NAME` |  |
| Cloudinary | `CLOUDINARY_API_KEY` |  |
| Cloudinary | `CLOUDINARY_API_SECRET` |  |
| Twilio | `TWILIO_ACCOUNT_SID` |  |
| Twilio | `TWILIO_API_KEY_SID` |  |
| Twilio | `TWILIO_API_KEY_SECRET` |  |
| Pagamento | `PAYMENT_PROVIDER` |  |
| Pagamento | `PAYMENT_SECRET_KEY` |  |
| Pagamento | `PAYMENT_WEBHOOK_SECRET` |  |

## 16. Pendencias para Decisao do Cliente

Antes da implantacao de producao, o cliente deve confirmar:

1. Qual dominio oficial sera usado.
2. Qual provedor de pagamento sera usado para planos e assinaturas.
3. Se tera assinatura digital com Clicksign ja no MVP.
4. Se Twilio sera usado para SMS, WhatsApp, OTP ou todos.
5. Se Cloudinary armazenara apenas midia ou tambem anexos/documentos.
6. Se a producao sera 100% Azure ou combinada com outros provedores.
7. Quem sera o administrador titular de cada conta.
8. Quem aprova custos recorrentes.
9. Quem recebe alertas operacionais e financeiros.

## 17. Recomendacao Final

Para evitar dependencia de contas pessoais, todas as ferramentas devem ser criadas em nome da empresa do cliente, com pelo menos dois administradores internos e acesso tecnico concedido por convite. Secrets devem ficar em cofre seguro e ser rotacionados sempre que houver troca de equipe, suspeita de vazamento ou migracao de ambiente.
