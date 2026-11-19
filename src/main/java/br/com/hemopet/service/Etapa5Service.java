package br.com.hemopet.service;

import br.com.hemopet.dao.Etapa5Dao;

import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.util.List;

public class Etapa5Service {

    private final Etapa5Dao dao = new Etapa5Dao();

    public Integer consultarIdadeAnimal(int idAnimal) {
        if (idAnimal <= 0) {
            throw new ValidationException(
                    "Informe um ID de animal valido.");
        }

        return dao.consultarIdadeAnimal(idAnimal);
    }

    public String consultarStatusValidade(int idBolsa) {
        if (idBolsa <= 0) {
            throw new ValidationException(
                    "Informe um ID de bolsa valido.");
        }

        return dao.consultarStatusValidade(idBolsa);
    }

    public int descartarBolsasVencidas(LocalDate dataReferencia) {
        return dao.descartarBolsasVencidas(dataReferencia);
    }

    public List<DefaultTableModel> processarSolicitacao(int idSolicitacao) {
        if (idSolicitacao <= 0) {
            throw new ValidationException(
                    "Informe um ID de solicitacao valido.");
        }

        return dao.processarSolicitacao(idSolicitacao);
    }

    public DefaultTableModel listLogBolsa() {
        return dao.listLogBolsa();
    }

    public DefaultTableModel listBolsasDescartadas() {
        return dao.listBolsasDescartadas();
    }
    public String testarTriggerColeta() {
        return dao.testarTriggerColeta();
    }
}
