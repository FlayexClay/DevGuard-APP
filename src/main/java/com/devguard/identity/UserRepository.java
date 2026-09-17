package com.devguard.identity;

import org.springframework.data.repository.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserRepository extends Repository<User, UUID> {
    Mono<User> save(User user);
    Mono<User> findBySubject(String subject);
}
