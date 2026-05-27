# Clean Architecture nos microservices

Os microservices do modulo Core/Auth/User seguem uma separacao em camadas para manter regra de negocio independente de HTTP, banco, JWT e frameworks.

## Camadas

- `domain`: modelos e conceitos do negocio. Nao depende de Quarkus, JDBC ou HTTP.
- `application`: casos de uso, comandos, portas e excecoes de aplicacao.
- `application.port.out`: contratos que a aplicacao precisa para persistencia, seguranca ou integracoes externas.
- `infrastructure`: adapters de saida, como JDBC, JWT, hash de senha, token interno e clientes HTTP.
- `interfaces.rest`: adapters de entrada REST, DTOs HTTP e traducao de erro para status code.

## Regras

- Resource REST nao acessa banco, nao gera JWT e nao conhece SQL.
- Repository nao valida caso de uso; ele persiste e consulta dados.
- Servico de aplicacao orquestra regras, validacoes e portas.
- Segredos e integracoes ficam na infraestrutura e entram por configuracao.
- Contratos entre microservices passam por portas, nao por classes REST de outro modulo.

## Modulo de identidade

- `user-service`: fonte de tenants, usuarios, roles e contador read-only.
- `auth-service`: orquestra autenticacao, emite JWT e gerencia refresh tokens/sessoes.
- `core-service`: resolve contexto tenant-aware a partir do JWT.

Servicos com dados tenant-aware devem resolver o tenant a partir do Bearer JWT emitido pelo `auth-service`. `tenantId` recebido por query, body ou header pode ser usado no maximo como parametro de rota a ser comparado contra o claim `tenant_id`, nunca como fonte de autorizacao. `CONTADOR` e leitura; escritas exigem `ADMIN` ou `VENDEDOR` conforme o caso de uso.

Para novos microservices, replique a mesma organizacao antes de adicionar regra de negocio.

## Camada de conectores no Core

A camada de conectores fica no `core-service` como contrato e orquestracao, nao como implementacao de marketplace. A regra e: Core conhece a porta `MarketplaceConnector`, mas nao conhece SDK, DTO, endpoint, autenticacao ou codigo especifico de Mercado Livre, Shopee, Amazon etc.

- `domain.model.connector`: modelos padronizados, como pedido, item, pagamento, taxa, nota fiscal, status e token.
- `application.port.out.MarketplaceConnector`: contrato que todo adapter de marketplace deve implementar.
- `application.service.ConnectorService`: resolve conector por nome e chama o contrato.
- `infrastructure.connector`: adapters concretos registrados via CDI.
- `interfaces.rest.ConnectorResource`: API REST para os modulos consumirem o contrato padrao.

Para adicionar um marketplace, crie um novo adapter em `infrastructure.connector` implementando `MarketplaceConnector`. Nao altere os casos de uso do Core para acomodar campos especificos de plataforma; normalize no adapter.

## Gateway API

O `gateway-api` e um adapter de borda. Ele nao deve conter regra de negocio dos microservices; seu papel e resolver a rota publica, aplicar politicas de borda e delegar a chamada HTTP por uma porta de saida.

- `interfaces.rest`: endpoints `/api`, traducao HTTP e erros publicos.
- `application`: roteamento, bloqueio de caminhos internos e contrato das portas.
- `domain`: modelo de rota downstream sem dependencia de Quarkus.
- `infrastructure`: catalogo configurado por env vars e cliente Quarkus REST Client.

Endpoints internos de servicos nao devem ser expostos pelo gateway. Quando um novo microservice precisar entrar na fachada, adicione uma rota explicita no catalogo configurado e um teste de encaminhamento.
