package com.example.urlshortener.auth.service;

import com.example.urlshortener.auth.dto.UserLoginRequest;
import com.example.urlshortener.auth.dto.UserLoginResponse;
import com.example.urlshortener.auth.dto.UserRegistrationRequest;
import com.example.urlshortener.auth.dto.UserRegistrationResponse;

public interface AuthService {

    UserRegistrationResponse register(UserRegistrationRequest request);

    UserLoginResponse login(UserLoginRequest request);
}
