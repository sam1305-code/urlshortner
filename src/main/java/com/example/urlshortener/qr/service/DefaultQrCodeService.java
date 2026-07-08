package com.example.urlshortener.qr.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.urlshortener.config.UrlShortenerProperties;
import com.example.urlshortener.url.exception.ShortUrlNotFoundException;
import com.example.urlshortener.url.repository.ShortUrlRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

@Service
public class DefaultQrCodeService implements QrCodeService {

    private static final int QR_CODE_SIZE_PIXELS = 300;
    private static final String IMAGE_FORMAT = "PNG";

    private final ShortUrlRepository shortUrlRepository;
    private final UrlShortenerProperties urlShortenerProperties;

    public DefaultQrCodeService(
            ShortUrlRepository shortUrlRepository,
            UrlShortenerProperties urlShortenerProperties) {
        this.shortUrlRepository = shortUrlRepository;
        this.urlShortenerProperties = urlShortenerProperties;
    }

    @Override
    public byte[] generateQrCode(UUID ownerId, String shortCode) {
        shortUrlRepository.findByShortCodeAndOwnerId(shortCode, ownerId)
                .filter(shortUrl -> !shortUrl.deleted())
                .orElseThrow(() -> new ShortUrlNotFoundException(shortCode));

        return generatePng(publicShortUrl(shortCode));
    }

    private byte[] generatePng(String content) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var bitMatrix = new QRCodeWriter().encode(
                    content,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE_PIXELS,
                    QR_CODE_SIZE_PIXELS);
            MatrixToImageWriter.writeToStream(bitMatrix, IMAGE_FORMAT, outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Unable to generate QR code.", exception);
        }
    }

    private String publicShortUrl(String shortCode) {
        return urlShortenerProperties.publicBaseUrl().replaceAll("/+$", "") + "/" + shortCode;
    }
}
