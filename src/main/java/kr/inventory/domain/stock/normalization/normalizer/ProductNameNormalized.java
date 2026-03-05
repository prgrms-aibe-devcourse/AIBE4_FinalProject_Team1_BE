package kr.inventory.domain.stock.normalization.normalizer;

public record ProductNameNormalized(
    String displayName,
    String productKey
) {
}
