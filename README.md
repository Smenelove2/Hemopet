# HemoPet App

Aplicacao desktop em Java para a Etapa 03 do projeto HemoPet.

## Tecnologias

- Java 17+
- Maven
- Swing
- JDBC
- MySQL
- JFreeChart

## Configuracao Do Banco

O app espera um banco MySQL chamado `hemopet`.

Antes de rodar a aplicacao, crie e popule esse banco usando os scripts SQL do projeto, caso eles tenham sido fornecidos separadamente:

- `01_criacao_tabelas_hemopet.sql`
- `Inserção.sql`
- `consultas.sql`

Esses scripts nao sao lidos automaticamente pela aplicacao; eles devem ser executados no MySQL antes de iniciar o app.

Configure o acesso em `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/hemopet?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true
db.user=root
db.password=
```

Tambem e possivel sobrescrever por variaveis de ambiente:

- `HEMOPET_DB_URL`
- `HEMOPET_DB_USER`
- `HEMOPET_DB_PASSWORD`

## Como Rodar

Dentro da pasta `hemopet-app`:

```bash
mvn clean compile
mvn exec:java
```

## Funcionalidades

- Dashboard com indicadores e graficos.
- CRUD de animais doadores.
- CRUD de hospitais veterinarios.
- Tela para executar as 4 consultas SQL exigidas.
- Visualizacao de coletas, bolsas, solicitacoes e itens de solicitacao.

## Observacoes

- Os comandos SQL estao explicitos nos DAOs, usando JDBC e `PreparedStatement`.
- As exclusoes podem ser bloqueadas pelo MySQL quando houver registros vinculados por chave estrangeira.
