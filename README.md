# HemoPet App

Aplicacao desktop em Java para a Etapa 03 do projeto HemoPet.

## Tecnologias

- Java 17+
- Maven
- Swing
- JDBC
- MySQL
- JFreeChart

## Requisitos

- Java 17 ou superior
- Maven 3.9 ou superior
- MySQL 8.0.16 ou superior

> O projeto nao e compativel com PostgreSQL. A porta `5432` normalmente pertence ao
> PostgreSQL; use a porta configurada no seu servidor MySQL (normalmente `3306`).

## Preparacao Do Banco

O app espera um banco MySQL chamado `hemopet`. Os scripts entregaveis estao na pasta
[`database`](database) e devem ser executados, nesta ordem:

1. `database/01_criacao_tabelas_hemopet.sql`
2. `database/02_insercao_dados.sql`
3. `database/03_consultas.sql` (consultas da Etapa 03; nao e necessario executa-lo para iniciar o app)

Exemplo pelo cliente de linha de comando do MySQL:

```bash
mysql -u root -p < database/01_criacao_tabelas_hemopet.sql
mysql -u root -p < database/02_insercao_dados.sql
```

Os scripts nao sao executados automaticamente pela aplicacao para evitar sobrescrever
um banco existente.

## Configuracao Da Conexao

Configure o acesso em `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true
db.user=root
db.password=
```

Por seguranca, nao grave uma senha real no Git. Prefira sobrescrever a configuracao com
variaveis de ambiente:

- `HEMOPET_DB_URL`
- `HEMOPET_DB_USER`
- `HEMOPET_DB_PASSWORD`

No PowerShell, por exemplo:

```powershell
$env:HEMOPET_DB_URL = "jdbc:mysql://localhost:3306/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"
$env:HEMOPET_DB_USER = "root"
$env:HEMOPET_DB_PASSWORD = "sua-senha"
```

## Como Compilar E Rodar

Na raiz do projeto:

```bash
mvn clean package
mvn exec:java
```

O pacote compilado e gerado em `target/hemopet-app-1.0.0.jar`. A execucao recomendada
continua sendo `mvn exec:java`, pois o Maven fornece as dependencias do driver MySQL e
dos graficos.

## Funcionalidades

- Dashboard com indicadores e graficos.
- CRUD de animais doadores.
- CRUD de hospitais veterinarios.
- Tela para executar as 4 consultas SQL exigidas.
- Visualizacao de coletas, bolsas, solicitacoes e itens de solicitacao.

## Observacoes

- Os comandos SQL estao explicitos nos DAOs, usando JDBC e `PreparedStatement`.
- As exclusoes podem ser bloqueadas pelo MySQL quando houver registros vinculados por chave estrangeira.
- O cadastro de animais depende de tutores previamente carregados pelo script de insercao.
