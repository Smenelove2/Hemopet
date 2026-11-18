
package br.com.hemopet.service;

import br.com.hemopet.dao.PessoaDao;
import br.com.hemopet.model.Pessoa;

public class PessoaService {

    private final PessoaDao dao = new PessoaDao();

    public void create(Pessoa pessoa) {
        validate(pessoa);
        dao.insert(pessoa);
    }

    public void update(Pessoa pessoa) {
        validate(pessoa);
        dao.update(pessoa);
    }

    public void delete(String cpf) {
        requireText(cpf, "Informe o CPF da pessoa.");

        if (!cpf.trim().matches("\\d{11}")) {
            throw new ValidationException(
                    "O CPF deve possuir exatamente 11 digitos."
            );
        }

        dao.delete(cpf.trim());
    }

    private void validate(Pessoa pessoa) {
        if (pessoa == null) {
            throw new ValidationException("Informe os dados da pessoa.");
        }

        requireText(pessoa.getCpf(), "Informe o CPF.");
        requireText(pessoa.getNome(), "Informe o nome.");
        requireText(pessoa.getRua(), "Informe a rua.");
        requireText(pessoa.getNumero(), "Informe o numero.");
        requireText(pessoa.getBairro(), "Informe o bairro.");
        requireText(pessoa.getCidade(), "Informe a cidade.");
        requireText(pessoa.getCep(), "Informe o CEP.");

        pessoa.setCpf(pessoa.getCpf().trim());
        pessoa.setNome(pessoa.getNome().trim());
        pessoa.setRua(pessoa.getRua().trim());
        pessoa.setNumero(pessoa.getNumero().trim());
        pessoa.setBairro(pessoa.getBairro().trim());
        pessoa.setCidade(pessoa.getCidade().trim());
        pessoa.setCep(pessoa.getCep().trim());

        if (!pessoa.getCpf().matches("\\d{11}")) {
            throw new ValidationException(
                    "O CPF deve possuir exatamente 11 digitos."
            );
        }

        if (pessoa.getNome().length() > 100) {
            throw new ValidationException(
                    "O nome deve possuir no maximo 100 caracteres."
            );
        }

        if (pessoa.getRua().length() > 100) {
            throw new ValidationException(
                    "A rua deve possuir no maximo 100 caracteres."
            );
        }

        if (pessoa.getNumero().length() > 10) {
            throw new ValidationException(
                    "O numero deve possuir no maximo 10 caracteres."
            );
        }

        if (pessoa.getBairro().length() > 60) {
            throw new ValidationException(
                    "O bairro deve possuir no maximo 60 caracteres."
            );
        }

        if (pessoa.getCidade().length() > 60) {
            throw new ValidationException(
                    "A cidade deve possuir no maximo 60 caracteres."
            );
        }

        if (!pessoa.getCep().matches("\\d{8}")) {
            throw new ValidationException(
                    "O CEP deve possuir exatamente 8 digitos."
            );
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(message);
        }
    }
}
