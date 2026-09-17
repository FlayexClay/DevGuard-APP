package com.devguard.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserProvisioningService {

    private static final Logger log = LoggerFactory.getLogger(UserProvisioningService.class);

    private static final List<String> ROLE_PRECEDENCE =
            List.of("ADMIN", "SECURITY_ANALYST", "DEVELOPER");

    private final UserRepository users;
    private final MembershipRepository memberships;
    private final Map<String, UUID> subjectToId = new ConcurrentHashMap<>();

    public UserProvisioningService(UserRepository users, MembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    public Mono<UUID> resolver(String subject, String email, String displayName, UUID organizationId, Set<String> roles) {

        UUID cached = subjectToId.get(subject);
        if (cached != null) {
            return Mono.just(cached);
        }

        return users.findBySubject(subject)
                .switchIfEmpty(Mono.defer(() -> createUser(subject, email, displayName)))
                .flatMap(user -> ensureMembership(user.getId(), organizationId, roles)
                        .thenReturn(user.getId()))
                .doOnNext(id -> subjectToId.put(subject, id));

    }

    private Mono<User> createUser(String subject, String email, String displayName) {
        return users.save(User.fromToken(subject, email, displayName))
                .doOnSuccess(u -> log.info("Usuario provisionado: {} ({})",
                        u.getSubject(), u.getEmail()))
                .onErrorResume(e -> users.findBySubject(subject)
                        .switchIfEmpty(Mono.error(e)));
    }

    private Mono<Membership> ensureMembership(UUID userId, UUID organizationId, Set<String> roles) {
        return memberships.findByOrganizationIdAndUserId(organizationId, userId)
                .switchIfEmpty(Mono.defer(() -> memberships
                        .save(Membership.of(organizationId, userId, highestRole(roles)))
                        .onErrorResume(e -> memberships
                                .findByOrganizationIdAndUserId(organizationId, userId)
                                .switchIfEmpty(Mono.error(e)))));
    }

    private static String highestRole(Set<String> roles) {
        return ROLE_PRECEDENCE.stream()
                .filter(roles::contains)
                .findFirst()
                .orElse("DEVELOPER");
    }
}
