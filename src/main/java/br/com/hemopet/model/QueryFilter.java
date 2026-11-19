package br.com.hemopet.model;

import br.com.hemopet.service.ValidationException;
import java.time.LocalDate;

public record QueryFilter(String text, String species, String status, String urgency, LocalDate start, LocalDate end) {
    public static final QueryFilter ALL = new QueryFilter("", null, null, null, null, null);
    public QueryFilter {
        text = text == null ? "" : text.trim();
        if (start != null && end != null && start.isAfter(end))
            throw new ValidationException("A data inicial deve ser anterior ou igual a data final.");
    }
}
