package com.example.urlshortener.auth.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.example.urlshortener.auth.model.UserAccount;
import com.example.urlshortener.auth.util.EmailNormalizer;

@Repository
@Profile("!couchbase")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> usersByEmail = new ConcurrentHashMap<>();

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(EmailNormalizer.normalize(email)));
    }

    @Override
    public boolean insertIfEmailAbsent(UserAccount userAccount) {
        UserAccount existingUser = usersByEmail.putIfAbsent(userAccount.email(), userAccount);
        return existingUser == null;
    }
}
