# Mudanças em relação à versão do Tomás

Base: [PR #1 de Nerolero](https://github.com/Smenelove2/Hemopet/pull/1), commit `7f58106`, já incorporado à main.

Mantidos os quatro CRUDs, os seis gráficos e as estatísticas de peso do Tomás. Sobre essa base:

- Corrigidos o DAO da Etapa 05 e a compilação em Java 17, com limpeza automática das classes antigas.
- Adicionados filtros no dashboard, busca nos cadastros e ordenação que preserva a seleção correta para edição.
- Disponibilizadas as oito consultas e duas views das Etapas 03 e 04 com filtros SQL parametrizados e carregamento em segundo plano.
- Integradas funções, procedimentos e triggers, com transações, rollback e demonstração de bloqueio de coleta.
- Ajustados scripts SQL, reinstalação de índices e instalação das atualizações sem recarregar os cadastros existentes.
- Adicionado o inicializador Windows, incluindo a correção da sondagem de MySQL no PowerShell 5.1.
- Atualizados README, verificações automatizadas e ZIP de entrega.

Validação já realizada: compilação Java 17, verificações de interface, execução direta de SQL no MySQL 8.0.43 e sondagem de conexão no PowerShell 5.1. A suíte JDBC completa e o ciclo Maven permanecem sem execução neste ambiente.
