package kr.inventory.domain.dining.service;

public interface QrGenerator {
    byte[] generate(String content, int width, int height);
}
