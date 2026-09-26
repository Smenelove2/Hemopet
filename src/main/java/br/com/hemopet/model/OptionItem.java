package br.com.hemopet.model;

public record OptionItem(String id, String label) {
    @Override
    public String toString() {
        return label;
    }
}
