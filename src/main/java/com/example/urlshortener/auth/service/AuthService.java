package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;

public interface AuthService {

    UserRegistrationResponse register(UserRegistrationRequest request);
}
