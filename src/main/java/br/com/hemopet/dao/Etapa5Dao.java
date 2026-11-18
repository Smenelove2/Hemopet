package br.com.hemopet.app;

import br.com.hemopet.dao.AnimalDoadorDao;
import br.com.hemopet.dao.DashboardDao;
import br.com.hemopet.dao.HospitalVeterinarioDao;
import br.com.hemopet.dao.QueryDao;
import br.com.hemopet.model.AnimalDoador;
import br.com.hemopet.model.HospitalVeterinario;
import br.com.hemopet.service.AnimalDoadorService;
import br.com.hemopet.service.HospitalVeterinarioService;
import br.com.hemopet.config.DatabaseConfig;
import br.com.hemopet.dao.VeterinarioDao;
import br.com.hemopet.model.Veterinario;
import br.com.hemopet.service.VeterinarioService;
import br.com.hemopet.dao.PessoaDao;
import br.com.hemopet.model.Pessoa;
import br.com.hemopet.service.PessoaService;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;


/**
 * Verificacao ponta a ponta sem abrir a interface grafica.
 *
 * <p>Usa a mesma configuracao HEMOPET_DB_* da aplicacao, executa as quatro consultas
 * e valida inclusao, alteracao e exclusao nas duas tabelas editaveis.</p>
 */
public final class DatabaseSmokeCheck {
    private static final String TEST_CNPJ = "99999999999999";
    private static final String TEST_ANIMAL_NAME = "Animal Teste CRUD";

    private DatabaseSmokeCheck() {
    }

    public static void main(String[] args) {
        verifyDashboardAndQueries();
        verifyHospitalCrud();
        verifyPessoaCrud();
        verifyVeterinarioCrud();
        verifyAnimalCrud();
        System.out.println("HemoPet smoke check: OK");
    }

    private static void verifyDashboardAndQueries() {
        DashboardDao dashboardDao = new DashboardDao();
        require(dashboardDao.countAnimals().value() > 0, "Dashboard sem animais.");
        require(dashboardDao.countHospitals().value() > 0, "Dashboard sem hospitais.");
        require(!dashboardDao.countAnimalsBySpecies().isEmpty(), "Grafico de especies vazio.");
        require(!dashboardDao.countBagsByStatus().isEmpty(), "Grafico de bolsas vazio.");
        require(!dashboardDao.countRequestsByUrgency().isEmpty(), "Grafico de urgencias vazio.");

        QueryDao queryDao = new QueryDao();
        require(queryDao.getQueryNames().size() == 4, "A aplicacao deve expor quatro consultas.");
        queryDao.getQueryNames().keySet().forEach(name -> {
            DefaultTableModel result = queryDao.execute(name);
            require(result.getColumnCount() > 0, "Consulta sem colunas: " + name);
            require(result.getRowCount() > 0, "Consulta sem resultados: " + name);
        });
    }

    private static void verifyHospitalCrud() {
        HospitalVeterinarioDao dao = new HospitalVeterinarioDao();
        HospitalVeterinarioService service = new HospitalVeterinarioService();
        Integer id = null;

        try {
            HospitalVeterinario hospital = new HospitalVeterinario();
            hospital.setCnpj(TEST_CNPJ);
            hospital.setNomeFantasia("Hospital Teste CRUD");
            hospital.setAtivo(true);
            service.save(hospital);

            id = findId(dao.listTableModel(), 1, TEST_CNPJ);
            require(id != null, "Falha na inclusao de hospital.");

            hospital.setIdHospital(id);
            hospital.setNomeFantasia("Hospital Teste Atualizado");
            hospital.setAtivo(false);
            service.save(hospital);
            require(findId(dao.listTableModel(), 2, "Hospital Teste Atualizado") != null,
                    "Falha na alteracao de hospital.");
        } finally {
            if (id == null) {
                id = findId(dao.listTableModel(), 1, TEST_CNPJ);
            }
            if (id != null) {
                service.delete(id);
            }
        }

        require(findId(dao.listTableModel(), 1, TEST_CNPJ) == null,
                "Falha na exclusao de hospital.");
    }

    private static void verifyAnimalCrud() {
        AnimalDoadorDao dao = new AnimalDoadorDao();
        AnimalDoadorService service = new AnimalDoadorService();
        Integer id = null;

        try {
            AnimalDoador animal = new AnimalDoador();
            animal.setNome(TEST_ANIMAL_NAME);
            animal.setEspecie("CAO");
            animal.setRaca("SRD");
            animal.setTipoSanguineo("DEA 1 NEGATIVO");
            animal.setPeso(new BigDecimal("25.00"));
            animal.setDataNascimento(LocalDate.of(2020, 1, 1));
            animal.setAutorizacaoDoacao(true);
            animal.setCpfTutor("10000000001");
            service.save(animal);

            id = findId(dao.listTableModel(), 1, TEST_ANIMAL_NAME);
            require(id != null, "Falha na inclusao de animal.");

            animal.setIdAnimal(id);
            animal.setNome("Animal Teste Atualizado");
            animal.setPeso(new BigDecimal("26.50"));
            service.save(animal);
            require(findId(dao.listTableModel(), 1, "Animal Teste Atualizado") != null,
                    "Falha na alteracao de animal.");
        } finally {
            if (id == null) {
                id = findId(dao.listTableModel(), 1, TEST_ANIMAL_NAME);
            }
            if (id != null) {
                service.delete(id);
            }
        }

        require(findId(dao.listTableModel(), 1, TEST_ANIMAL_NAME) == null
                        && findId(dao.listTableModel(), 1, "Animal Teste Atualizado") == null,
                "Falha na exclusao de animal.");
    }

    private static Integer findId(DefaultTableModel model, int searchColumn, String value) {
        for (int row = 0; row < model.getRowCount(); row++) {
            Object cell = model.getValueAt(row, searchColumn);
            if (value.equals(String.valueOf(cell))) {
                return Integer.valueOf(String.valueOf(model.getValueAt(row, 0)));
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }


    private static void verifyPessoaCrud() {
    PessoaDao dao = new PessoaDao();
    PessoaService service = new PessoaService();

    String cpf = findAvailableCpf();
    String testName = "Pessoa Teste " + cpf;
    String updatedName = "Pessoa Atualizada " + cpf;

    try {
        // Inclusao
        Pessoa pessoa = new Pessoa();
        pessoa.setCpf(cpf);
        pessoa.setNome(testName);
        pessoa.setRua("Rua Teste");
        pessoa.setNumero("100");
        pessoa.setBairro("Centro");
        pessoa.setCidade("Recife");
        pessoa.setCep("50000000");

        service.create(pessoa);

        require(
                containsValue(dao.listTableModel(), 0, cpf),
                "Falha na inclusao de pessoa."
        );

        // Alteracao
        pessoa.setNome(updatedName);
        pessoa.setRua("Rua Teste Atualizada");

        service.update(pessoa);

        require(
                containsValue(dao.listTableModel(), 1, updatedName),
                "Falha na alteracao de pessoa."
        );

    } finally {
        // Exclusao
        if (containsValue(dao.listTableModel(), 0, cpf)) {
            service.delete(cpf);
        }
    }

    require(
            !containsValue(dao.listTableModel(), 0, cpf),
            "Falha na exclusao de pessoa."
    );
}

private static void verifyVeterinarioCrud() {
    PessoaDao pessoaDao = new PessoaDao();
    PessoaService pessoaService = new PessoaService();

    VeterinarioDao dao = new VeterinarioDao();
    VeterinarioService service = new VeterinarioService();

    String cpf = findAvailableCpf();
    String suffix = Long.toUnsignedString(System.nanoTime());

    String testCrmv = "T" + suffix;
    String updatedCrmv = "A" + suffix;

    try {
        // Criacao da pessoa necessaria para o veterinario
        Pessoa pessoa = new Pessoa();
        pessoa.setCpf(cpf);
        pessoa.setNome("Pessoa Veterinario Teste " + cpf);
        pessoa.setRua("Rua Teste");
        pessoa.setNumero("100");
        pessoa.setBairro("Centro");
        pessoa.setCidade("Recife");
        pessoa.setCep("50000000");

        pessoaService.create(pessoa);

        require(
                containsValue(pessoaDao.listTableModel(), 0, cpf),
                "Falha na inclusao da pessoa para teste de veterinario."
        );

        // Inclusao do veterinario
        Veterinario veterinario = new Veterinario();
        veterinario.setCpf(cpf);
        veterinario.setCrmv(testCrmv);
        veterinario.setCpfSupervisor(null);

        service.create(veterinario);

        require(
                containsValue(dao.listTableModel(), 0, cpf),
                "Falha na inclusao de veterinario."
        );

        // Alteracao
        veterinario.setCrmv(updatedCrmv);

        service.update(veterinario);

        require(
                containsValue(dao.listTableModel(), 2, updatedCrmv),
                "Falha na alteracao de veterinario."
        );

    } finally {
        // Exclusao do veterinario antes da pessoa
        try {
            if (containsValue(dao.listTableModel(), 0, cpf)) {
                service.delete(cpf);
            }
        } finally {
            if (containsValue(pessoaDao.listTableModel(), 0, cpf)) {
                pessoaService.delete(cpf);
            }
        }
    }

    require(
            !containsValue(dao.listTableModel(), 0, cpf),
            "Falha na exclusao de veterinario."
    );

    require(
            !containsValue(pessoaDao.listTableModel(), 0, cpf),
            "Falha na exclusao da pessoa vinculada ao veterinario."
    );
}

private static String findAvailableCpf() {
    String sql = "SELECT 1 FROM pessoa WHERE cpf = ?";

    try (Connection connection = DatabaseConfig.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        for (int attempt = 0; attempt < 20; attempt++) {
            String cpf = String.format("%011d",ThreadLocalRandom.current().nextLong(100_000_000_000L));

            statement.setString(1, cpf);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return cpf;
                }
            }
        }

        throw new IllegalStateException("Nao foi possivel gerar um CPF disponivel para o teste.");

    } catch (SQLException exception) {
        throw new IllegalStateException("Nao foi possivel verificar disponibilidade do CPF.",exception);
    }
}

private static boolean containsValue(DefaultTableModel model, int searchColumn, String expectedValue) {
    for (int row = 0; row < model.getRowCount(); row++) {
        Object cell = model.getValueAt(row, searchColumn);

        if (expectedValue.equals(String.valueOf(cell))) {
            return true;
        }
    }

    return false;
    }
}


