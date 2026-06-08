# Audit de Alinhamento - BraSeller

Atualizado em: 2026-06-08

Este audit consolida o alinhamento entre a visao do produto e o que esta implementado no sistema. A documentacao tecnica completa e os diagramas UML profissionais estao em:

[06-estado-atual-e-planta-uml.md](06-estado-atual-e-planta-uml.md)

## Status Executivo

| Area | Status | Observacao |
|------|--------|------------|
| Marketplaces / vendas | Feito | Core sincroniza vendas e envia lancamentos para Reporting. |
| Split de taxas e frete | Feito | Taxas e frete padronizados por marketplace. |
| Despesas manuais | Feito | CRUD com comprovante obrigatorio via Cloudinary. |
| OFX bancario | Feito/parcial | Importa OFX e soma despesas bancarias na DRE; Open Finance real falta. |
| Estoque / XML NF-e fornecedor | Feito | XML alimenta estoque e custo unitario. |
| CMV por venda/SKU | Feito | Venda com SKU gera EXIT e entra no CMV. |
| Estorno CMV/estoque | Feito | CANCELLED/REFUNDED geram SALE_REVERSAL. |
| Estorno de receita cancelada | Feito | Receita, recebido, taxa e a receber sao zerados em cancelamento/reembolso. |
| DRE | Feito | Receita, taxas, frete, impostos, CMV, despesas, bancos, resultado liquido e lucro distribuivel. |
| Regime tributario automatico | Feito | Simples, Presumido e Real calculam aliquota efetiva automaticamente. |
| Lucro disponivel | Feito | Fechamento libera saldo; distribuicoes consomem saldo disponivel. |
| Painel contador SaaS | Feito | Contador visualiza multiplos clientes vinculados. |
| BPO multi-cliente | Feito | Painel BPO com clientes vinculados, carteira global para operador interno e assinatura de fechamentos em lote. |
| Modo contador somente leitura | Feito | Telas operacionais ficam bloqueadas com cadeado animado. |
| CNPJ / Receita Federal | Feito/parcial | Consulta BrasilAPI por CNPJ em configuracoes. |
| Assinatura Clicksign | Feito/parcial | Fechamento e webhook existem; ICP-Brasil nativo falta. |
| API Nota Fiscal / SEFAZ | Falta | Ainda nao emite NF-e nem calcula imposto real por nota emitida. |
| Balanco Patrimonial | Falta | Ainda nao ha Ativo, Passivo e Patrimonio Liquido. |
| Webhooks marketplace | Falta/parcial | Cancelamentos entram por sync/conector; webhook real por marketplace falta. |

## Narrativa de Demo Recomendada

1. Mostrar conectores e sincronizacao.
2. Mostrar lancamentos normalizados.
3. Mostrar estoque/CMV por SKU.
4. Mostrar DRE com receita, taxas, frete, impostos, CMV, banco e lucro liquido.
5. Trocar regime tributario e mostrar mudanca na aliquota efetiva.
6. Mostrar fechamento assinado e lucro disponivel.
7. Mostrar painel BPO com clientes, carteira global e assinatura em lote.
8. Entrar como contador e mostrar telas bloqueadas com cadeado animado.
