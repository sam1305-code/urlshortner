package com.example.urlshortener.auth.repository;

import java.util.Optional;

import com.example.urlshortener.auth.model.UserAccount;

public interface UserAccountRepository {

    Optional<UserAccount> findByEmail(String email);

    boolean insertIfEmailAbsent(UserAccount userAccount);
}
