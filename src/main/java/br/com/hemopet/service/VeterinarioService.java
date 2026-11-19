package br.com.hemopet.service;

import br.com.hemopet.dao.VeterinarioDao;
import br.com.hemopet.model.Veterinario;

public class VeterinarioService {

    private final VeterinarioDao dao = new VeterinarioDao();

    public void create(Veterinario veterinario) {
        validate(veterinario);
        dao.insert(veterinario);
    }

    public void update(Veterinario veterinario) {
        validate(veterinario);
        dao.update(veterinario);
    }

    public void delete(String cpf) {
        requireText(cpf, "Informe o CPF do veterinario."); // Lembrar de fazer as validações do cpf certinhas
        if (!cpf.trim().matches("\\d{11}")) {
            throw new ValidationException("O CPF deve possuir exatamente 11 digitos.");
        }
        dao.delete(cpf.trim());
    }

private void validate(Veterinario veterinario) {
    if (veterinario == null) {
        throw new ValidationException("Informe os dados do veterinario.");
    }

    requireText(veterinario.getCpf(), "Informe o CPF do veterinario.");
    requireText(veterinario.getCrmv(), "Informe o CRMV do veterinario.");

    veterinario.setCpf(veterinario.getCpf().trim());
    veterinario.setCrmv(veterinario.getCrmv().trim());

    if (!veterinario.getCpf().matches("\\d{11}")) {
        throw new ValidationException("O CPF deve possuir exatamente 11 digitos.");
    }

    if (veterinario.getCrmv().length() > 20) {
        throw new ValidationException("O CRMV deve possuir no maximo 20 caracteres.");
    }

    if (veterinario.getCpfSupervisor() != null
            && !veterinario.getCpfSupervisor().isBlank()) {

        veterinario.setCpfSupervisor(veterinario.getCpfSupervisor().trim());

        if (!veterinario.getCpfSupervisor().matches("\\d{11}")) {
            throw new ValidationException("O CPF do supervisor deve possuir 11 digitos.");
        }

        if (veterinario.getCpfSupervisor().equals(veterinario.getCpf())) {
            throw new ValidationException("O veterinario nao pode ser seu proprio supervisor.");
        }

    } else {
        veterinario.setCpfSupervisor(null);
    }
}
    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
