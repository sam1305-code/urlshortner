package com.example.urlshortener.qr.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.urlshortener.config.UrlShortenerProperties;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.model.ShortUrl;
import com.example.urlshortener.url.repository.InMemoryShortUrlRepository;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class DefaultQrCodeServiceTest {

    private ShortUrlRepository shortUrlRepository;
    private DefaultQrCodeService qrCodeService;

    @BeforeEach
    void setUp() {
        shortUrlRepository = new InMemoryShortUrlRepository();
        qrCodeService = new DefaultQrCodeService(
                shortUrlRepository,
                new UrlShortenerProperties("https://sho.rt/"));
    }

    @Test
    void generateQrCodeReturnsPngEncodingPublicShortUrlForOwner() throws Exception {
        UUID ownerId = UUID.fromString("4c2b0f9d-87ef-4199-a75b-1c8d85c0774a");
        shortUrlRepository.insertIfShortCodeAbsent(shortUrl("abc123XY", ownerId, false));

        byte[] qrCode = qrCodeService.generateQrCode(ownerId, "abc123XY");

        assertThat(qrCode).startsWith(new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47});
        assertThat(decode(qrCode)).isEqualTo("https://sho.rt/abc123XY");
    }

    @Test
    void generateQrCodeRejectsUrlsOwnedByAnotherUser() {
        UUID ownerId = UUID.fromString("4c2b0f9d-87ef-4199-a75b-1c8d85c0774a");
        UUID otherOwnerId = UUID.fromString("96e36d43-c7c4-45d7-bd31-18aef918c5b7");
        shortUrlRepository.insertIfShortCodeAbsent(shortUrl("abc123XY", otherOwnerId, false));

        assertThatThrownBy(() -> qrCodeService.generateQrCode(ownerId, "abc123XY"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    @Test
    void generateQrCodeRejectsDeletedUrls() {
        UUID ownerId = UUID.fromString("4c2b0f9d-87ef-4199-a75b-1c8d85c0774a");
        shortUrlRepository.insertIfShortCodeAbsent(shortUrl("abc123XY", ownerId, true));

        assertThatThrownBy(() -> qrCodeService.generateQrCode(ownerId, "abc123XY"))
                .isInstanceOf(ShortUrlNotFoundException.class);
    }

    private String decode(byte[] qrCode) throws Exception {
        var image = ImageIO.read(new ByteArrayInputStream(qrCode));
        var source = new BufferedImageLuminanceSource(image);
        var bitmap = new BinaryBitmap(new HybridBinarizer(source));

        return new MultiFormatReader().decode(bitmap).getText();
    }

    private ShortUrl shortUrl(String shortCode, UUID ownerId, boolean deleted) {
        return new ShortUrl(
                UUID.randomUUID(),
                shortCode,
                "https://example.com/docs",
                ownerId,
                Instant.parse("2026-07-08T10:15:30Z"),
                null,
                deleted);
    }
}
