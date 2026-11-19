package br.com.hemopet.model;

import br.com.hemopet.service.ValidationException;
import java.time.LocalDate;

public record DashboardFilter(String species, Boolean authorized, LocalDate start, LocalDate end) {
    public static final DashboardFilter ALL = new DashboardFilter(null, null, null, null);

    public DashboardFilter {
        if (species != null && !species.equals("CAO") && !species.equals("GATO"))
            throw new ValidationException("Especie invalida.");
        if (start != null && end != null && start.isAfter(end))
            throw new ValidationException("A data inicial deve ser anterior ou igual a data final.");
    }
}
