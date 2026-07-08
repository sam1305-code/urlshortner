package com.example.urlshortener.qr.service;

import java.util.UUID;

public interface QrCodeService {

    byte[] generateQrCode(UUID ownerId, String shortCode);
}
