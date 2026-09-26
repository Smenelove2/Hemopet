package br.com.hemopet.service;

import br.com.hemopet.dao.AnimalDoadorDao;
import br.com.hemopet.model.AnimalDoador;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AnimalDoadorService {
    private final AnimalDoadorDao dao = new AnimalDoadorDao();

    public void save(AnimalDoador animal) {
        validate(animal);
        if (animal.getIdAnimal() == null) {
            dao.insert(animal);
        } else {
            dao.update(animal);
        }
    }

    public void delete(int idAnimal) {
        dao.delete(idAnimal);
    }

    private void validate(AnimalDoador animal) {
        requireText(animal.getNome(), "Informe o nome do animal.");
        requireText(animal.getEspecie(), "Informe a especie.");
        requireText(animal.getRaca(), "Informe a raca.");
        requireText(animal.getTipoSanguineo(), "Informe o tipo sanguineo.");
        requireText(animal.getCpfTutor(), "Informe o tutor.");

        if (!animal.getEspecie().equals("CAO") && !animal.getEspecie().equals("GATO")) {
            throw new ValidationException("A especie deve ser CAO ou GATO.");
        }
        if (animal.getPeso() == null || animal.getPeso().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("O peso deve ser maior que zero.");
        }
        if (animal.getDataNascimento() == null || animal.getDataNascimento().isBefore(LocalDate.of(1990, 1, 1))) {
            throw new ValidationException("A data de nascimento deve ser a partir de 1990-01-01.");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
