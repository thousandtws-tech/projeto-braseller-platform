# Documentacao de Arquitetura - Brasaller Sistema Modular

Documentacao tecnica da plataforma Brasaller, atualizada em Junho/2026.

## Equipe Responsavel

| Papel | Nome |
|-------|------|
| Agencia de Desenvolvimento | **Clarituz** |
| CEO | **Jerferson** |
| Engenheiro Full Stack | **Vinicius Moreira** |

## Indice

| Arquivo | Conteudo |
|---------|----------|
| [01-visao-geral.md](01-visao-geral.md) | Stack, diagrama C4, componentes e padroes arquiteturais |
| [02-microservices-uml.md](02-microservices-uml.md) | Diagrama de classes UML por servico e deployment Azure |
| [03-banco-de-dados-erd.md](03-banco-de-dados-erd.md) | ERD de bancos de dados por servico |
| [04-fluxos-e-sequencias.md](04-fluxos-e-sequencias.md) | Diagramas de sequencia dos fluxos principais |
| [05-seguranca-e-integracoes.md](05-seguranca-e-integrações.md) | Modelo de seguranca, integracoes externas e env vars |
| [06-estado-atual-e-planta-uml.md](06-estado-atual-e-planta-uml.md) | Estado atual implementado, lacunas, roteiro de demo e planta UML profissional |

## Resumo Rapido

**Plataforma:** SaaS multi-tenant para gestao financeira e contabil de vendedores em marketplaces  
**Backend:** microservicos Quarkus 3.35.4 / Java 21  
**Frontend:** Next.js 16 + TypeScript + Server Actions  
**Banco:** PostgreSQL Neon, com separacao por servico  
**Cloud:** Azure Container Apps + Azure Container Registry + Terraform  
**Auth:** Keycloak OAuth2/OIDC + JWT  

## Servicos

| Servico | Porta | Funcao |
|---------|-------|--------|
| gateway-api | 8080 | Proxy e roteamento |
| auth-service | 8085 | Login, registro, JWT e OAuth Google |
| user-service | 8084 | Tenants, usuarios, roles, contador e CNPJ |
| billing-service | 8082 | Planos e assinaturas |
| core-service | 8081 | Conectores marketplace, pedidos, taxas e frete |
| reporting-service | 8087 | Dashboard, DRE, despesas, estoque, CMV, OFX, lucro disponivel e BPO |
| notification-service | 8083 | Emails, alertas e notificacoes in-app |
| apps/client | 3000 | Frontend Next.js do lojista e contador |

> Todos os diagramas usam Mermaid e podem ser renderizados no GitHub, GitLab, Notion ou VS Code com extensao Mermaid Preview.
