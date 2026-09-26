package br.com.hemopet.service;

import br.com.hemopet.dao.HospitalVeterinarioDao;
import br.com.hemopet.model.HospitalVeterinario;

public class HospitalVeterinarioService {
    private final HospitalVeterinarioDao dao = new HospitalVeterinarioDao();

    public void save(HospitalVeterinario hospital) {
        validate(hospital);
        if (hospital.getIdHospital() == null) {
            dao.insert(hospital);
        } else {
            dao.update(hospital);
        }
    }

    public void delete(int idHospital) {
        dao.delete(idHospital);
    }

    private void validate(HospitalVeterinario hospital) {
        if (hospital.getCnpj() == null || !hospital.getCnpj().matches("[0-9]{14}")) {
            throw new ValidationException("O CNPJ deve conter exatamente 14 digitos.");
        }
        if (hospital.getNomeFantasia() == null || hospital.getNomeFantasia().isBlank()) {
            throw new ValidationException("Informe o nome fantasia.");
        }
    }
}
