# HemoPet — Etapa 06

Aplicação desktop em Java 17, Swing, JDBC, MySQL e JFreeChart para um banco de sangue veterinário.
Esta versão integra o código do [PR #1 de Nerolero](https://github.com/Smenelove2/Hemopet/pull/1), no commit `7f58106`, com as correções e complementos da Etapa 06.

As diferenças em relação à versão do Tomás estão em [RESUMO_ALTERACOES_ETAPA06.md](RESUMO_ALTERACOES_ETAPA06.md).

## Funcionalidades

| Requisito | Implementação |
| --- | --- |
| CRUD de pelo menos quatro tabelas | Pessoas, veterinários, animais doadores e hospitais |
| Integração com dados reais | SQL explícito nos DAOs, JDBC e `PreparedStatement` |
| Indicadores | Animais, pessoas, hospitais, veterinários, coletas, solicitações pendentes, bolsas em estoque e percentual de animais autorizados |
| Estatísticas de peso | Média, mediana, moda, variância populacional e desvio padrão populacional |
| Seis gráficos | Espécies, status das bolsas, urgência das solicitações, aprovação das coletas, distribuição dos pesos e volume mensal coletado |
| Interatividade | Filtros no dashboard, detalhes ao passar o mouse, menu dos gráficos, zoom nos eixos disponíveis e exportação |
| Etapas 03 e 04 | Oito consultas e duas views na aba Consultas, com filtros SQL parametrizados |
| Etapa 05 | Duas funções, dois procedimentos, histórico do trigger de bolsas e demonstração do bloqueio de coleta sem autorização |
| Busca nos cadastros | Busca literal em todas as colunas e ordenação das tabelas, mantendo a seleção correta para edição |
| Dados gerais | Consulta de coletas, bolsas, solicitações e itens de solicitação |

A moda apresenta o menor peso entre os empatados na maior frequência; se não houver repetição, mostra N/A.
Os formulários validam os dados antes do SQL. As chaves estrangeiras preservam a integridade dos registros vinculados.

## Requisitos de execução

- JDK 17 ou superior, com `javac`.
- MySQL Server 8.0.16 ou superior.
- Maven 3.9+ para a execução manual. O inicializador Windows compila sem Maven.
- Internet no primeiro uso para baixar o driver MySQL 9.0.0 e JFreeChart 1.5.5.

O projeto usa MySQL. PostgreSQL e sua porta usual, 5432, não são compatíveis com estes scripts.

## Início rápido no Windows

Dê dois cliques em `iniciar-hemopet.cmd`, ou execute na raiz:

```powershell
.\iniciar-hemopet.ps1
```

O inicializador encontra o JDK instalado, prepara um MySQL isolado na porta 3308, cria o banco e carrega os dados de exemplo somente se o banco ainda não existir. Nas próximas execuções, preserva os cadastros e instala apenas atualizações SQL cujo conteúdo mudou.

As dependências, classes compiladas e dados locais ficam em `.hemopet-runtime`, fora do Git.
A compilação remove automaticamente suas classes antigas antes de gerar as novas.
Não é necessário apagar `target` manualmente.

Para preparar o banco e compilar sem abrir a janela:

```powershell
.\iniciar-hemopet.ps1 -Preparar
```

## Preparação manual do banco

Para uma instalação nova, execute os scripts nesta ordem:

1. `database/01_criacao_tabelas_hemopet.sql`
2. `database/02_insercao_dados.sql`
3. `database/04_consultas_visoes_indices.sql`
4. `database/etapa5/01_funcoes.sql`
5. `database/etapa5/02_procedimentos.sql`
6. `database/etapa5/03_triggers.sql`

`database/03_consultas.sql` contém as consultas da Etapa 03 e pode ser executado separadamente.
Em um banco que já possui tabelas e dados, aplique somente os scripts das Etapas 04 e 05; eles podem ser reaplicados.
O inicializador controla essas atualizações por checksum na tabela `app_schema_migration`.

No MySQL Workbench, abra cada arquivo e execute seu conteúdo completo, incluindo os comandos `DELIMITER` das rotinas.
Os scripts de criação e carga inicial não são reaplicados automaticamente a um banco existente.

## Configuração da conexão

Na execução manual, configure `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true
db.user=root
db.password=
```

Prefira fornecer as credenciais por variáveis de ambiente:

```powershell
$env:HEMOPET_DB_URL = "jdbc:mysql://localhost:3306/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"
$env:HEMOPET_DB_USER = "root"
$env:HEMOPET_DB_PASSWORD = "sua-senha"
```

Não grave senhas reais no Git. O inicializador Windows usa sua própria instância local em 127.0.0.1:3308, usuário root e senha vazia.

## Compilar e executar com Maven

```powershell
mvn compile exec:java
```

Para gerar `target/hemopet-app-1.0.0.jar`:

```powershell
mvn package
```

O Maven limpa seus resultados anteriores na fase `initialize` e compila explicitamente com `--release 17`.
Isso evita reutilizar classes antigas de outras versões de Java e elimina a etapa manual de remoção de `target`.
Use `mvn compile exec:java` para carregar também as dependências do driver e dos gráficos.

## Filtros do dashboard

Escolha os valores e pressione **Aplicar / atualizar**. **Limpar** restaura todos os dados.
O dashboard também é atualizado ao voltar para sua aba.

| Filtro | Alcance |
| --- | --- |
| Espécie e autorização de doação | Animais, estatísticas de peso e seus vínculos com coletas, bolsas e solicitações |
| Data inicial e final | Data da coleta para coletas e bolsas; data da solicitação para solicitações |
| Totais gerais | Pessoas, hospitais e veterinários permanecem identificados como indicadores gerais |

Datas usam AAAA-MM-DD e incluem o dia final inteiro. Campos vazios deixam o período aberto.
Quando houver filtro de animal, uma solicitação participa se possuir ao menos um item ligado a um animal correspondente; cada solicitação é contada uma única vez.
Os carregamentos do dashboard e das consultas acontecem em segundo plano, e uma resposta antiga não substitui uma seleção mais recente.
Gráficos sem resultados mostram uma indicação de ausência de dados.

## Consultas e views

A aba Consultas habilita somente os filtros aplicáveis ao relatório selecionado.
A busca é literal, sem interpretar expressões regulares ou SQL.

| Consulta | Filtros disponíveis |
| --- | --- |
| Etapa 03 — Animais por espécie | Busca e espécie |
| Etapa 03 — Animais e tutores | Busca e espécie |
| Etapa 03 — Animais acima da média de peso | Busca e espécie |
| Etapa 03 — Tutores com cão e gato | Busca do tutor |
| Etapa 04 — Tutores sem animais (anti join) | Busca por CPF, nome ou cidade |
| Etapa 04 — Hospitais e solicitações (full outer join) | Busca, urgência e período |
| Etapa 04 — Coletas acima da média de volume | Busca, espécie e período |
| Etapa 04 — Veterinários com coletas aprovadas | Busca por nome, CPF ou CRMV |
| View — Rastreabilidade da doação | Busca, espécie, status da bolsa e período da coleta |
| View — Solicitações por hospital | Busca, status da bolsa, urgência e período da solicitação |

A média usada nas consultas “acima da média” continua sendo a média geral, como nos scripts originais.
O relatório de tutores com cão e gato conserva as duas espécies para preservar o significado da consulta.
Resultados vazios são válidos, inclusive no anti join.

## Funções, procedimentos e triggers

Na aba **Etapa 05**:

- **Funções:** consulte idade por ID do animal e validade por ID da bolsa.
- **Procedimentos:** descarte bolsas vencidas com data de referência opcional; processe uma solicitação pendente e visualize o resumo e os problemas em tabelas separadas.
- **Triggers:** consulte o histórico de alterações das bolsas e demonstre o bloqueio de uma coleta sem autorização.

Os procedimentos são chamados por `CallableStatement` dentro de transações.
Falhas SQL revertem as alterações e os logs da operação.
Uma solicitação sem itens não é marcada como atendida.
A demonstração do trigger sempre executa rollback, mesmo quando a regra estiver ausente.

O cadastro de veterinários depende de uma pessoa já cadastrada.
O cadastro de animais usa tutores previamente carregados no banco.
As tabelas são somente leitura; alterações devem ser feitas pelos formulários.

## Verificação

Para compilar e executar a suíte completa no Windows:

```powershell
.\verificar-hemopet.ps1
```

O script cria um banco temporário `hemopet_test_*`, carrega os scripts, executa as verificações e remove esse banco ao terminar.
Os testes de integração recusam uma URL de banco que não tenha esse prefixo.
Os cadastros do banco principal são preservados.

Cobertura:

- Inclusão, consulta, alteração e exclusão dos quatro cadastros.
- Oito consultas e duas views, busca literal e filtros.
- Contagens e estatísticas reais por espécie/autorização e período vazio.
- Limite inclusivo do dia final e seis gráficos carregados.
- Funções, parâmetro OUT, múltiplos resultados do cursor, atendimento e descarte.
- Triggers e rollback após uma falha SQL injetada no segundo item.
- Busca/ordenação da interface, manutenção do filtro após atualizar a tabela e validação de entrada.

Para verificar somente a interface e as validações, sem MySQL:

```powershell
mvn test-compile exec:java "-Dexec.mainClass=br.com.hemopet.app.OfflineInterfaceCheck" "-Dexec.classpathScope=test"
```

Na revisão desta implementação, os fontes principais e de teste foram compilados com `javac --release 17` e a verificação de interface passou.
Também passaram a execução direta dos scripts e dos SQLs filtrados no MySQL 8.0.43 e a sondagem de conexão no PowerShell 5.1. A suíte JDBC completa continua sem execução: o executor desta sessão bloqueou o acesso local ao MySQL.
Maven não estava instalado neste ambiente, portanto seu ciclo não foi executado aqui.
