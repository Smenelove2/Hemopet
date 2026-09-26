package br.com.hemopet.app;

import br.com.hemopet.dao.AnimalDoadorDao;
import br.com.hemopet.dao.DashboardDao;
import br.com.hemopet.dao.HospitalVeterinarioDao;
import br.com.hemopet.dao.QueryDao;
import br.com.hemopet.model.AnimalDoador;
import br.com.hemopet.model.HospitalVeterinario;
import br.com.hemopet.service.AnimalDoadorService;
import br.com.hemopet.service.HospitalVeterinarioService;

import javax.swing.table.DefaultTableModel;
import java.math.BigDecimal;
import java.time.LocalDate;

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
}
