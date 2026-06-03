# Documentação de Arquitetura — Brasaller Sistema Modular

Documentação técnica completa da plataforma Brasaller, gerada em Junho/2026.

## Equipe Responsável

| Papel | Nome |
|-------|------|
| Agencia de Desenvolvimento | **Clarituz** |
| CEO | **Jerferson** |
| Engenheiro Full Stack | **Vinicius Moreira** |

## Índice

| Arquivo | Conteúdo |
|---------|---------|
| [01-visao-geral.md](01-visao-geral.md) | Stack, diagrama C4, componentes, padrões arquiteturais |
| [02-microservices-uml.md](02-microservices-uml.md) | Diagrama de classes UML por serviço + deployment Azure |
| [03-banco-de-dados-erd.md](03-banco-de-dados-erd.md) | ERD de todos os bancos de dados por serviço |
| [04-fluxos-e-sequencias.md](04-fluxos-e-sequencias.md) | Diagramas de sequência dos 10 fluxos principais |
| [05-seguranca-e-integrações.md](05-seguranca-e-integrações.md) | Modelo de segurança, integrações externas, env vars |

## Resumo Rápido

**Plataforma:** SaaS multi-tenant para gestão financeira de vendedores em marketplaces  
**Backend:** 7 microserviços Quarkus 3.35.4 / Java 21  
**Frontend:** Angular 18+ (standalone)  
**Banco:** PostgreSQL Neon (1 banco por serviço)  
**Cloud:** Azure Container Apps + Terraform  
**Auth:** Keycloak OAuth2/OIDC + JWT  

### Serviços

| Serviço | Porta | Função |
|---------|-------|--------|
| gateway-api | 8080 | Proxy/roteamento |
| auth-service | 8085 | Login, JWT, OAuth Google |
| user-service | 8084 | Tenants, usuários, roles |
| billing-service | 8082 | Planos, assinaturas |
| core-service | 8081 | Conectores marketplace, pedidos |
| reporting-service | 8087 | Dashboard, DRE, relatórios |
| notification-service | 8083 | Emails, alertas, in-app |

> Todos os diagramas usam [Mermaid](https://mermaid.js.org/) — renderizados no GitHub, GitLab, Notion e VS Code (extensão Mermaid Preview).
