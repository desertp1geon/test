package com.example.hexmap.subscription;

import java.util.List;

import com.example.hexmap.user.ApplicationUser;
import com.example.hexmap.user.ApplicationUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user is always taken from the verified JWT. No endpoint accepts a user id.
 */
@RestController
@RequestMapping("/api")
public class SubscriptionController {

    public record SubscribeRequest(
            @NotNull @Pattern(regexp = "^[0-9a-f]{15}$", message = "Invalid H3 index") String h3Index) {
    }

    public record MeDto(String displayName, List<String> roles, SubscriptionDto subscription) {
    }

    private final ApplicationUserService users;
    private final SubscriptionService subscriptions;

    public SubscriptionController(ApplicationUserService users, SubscriptionService subscriptions) {
        this.users = users;
        this.subscriptions = subscriptions;
    }

    /** Profile and current subscription: lets the client restore its state after re-login. */
    @GetMapping("/me")
    public MeDto me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        ApplicationUser user = users.resolve(jwt);
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new MeDto(user.displayName(), roles, subscriptions.current(user.id()).orElse(null));
    }

    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionDto> current(@AuthenticationPrincipal Jwt jwt) {
        ApplicationUser user = users.resolve(jwt);
        return subscriptions.current(user.id())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /** Create or change the primary monitoring zone. */
    @PutMapping("/subscription")
    public SubscriptionDto subscribe(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SubscribeRequest request) {
        ApplicationUser user = users.resolve(jwt);
        return subscriptions.subscribe(user.id(), request.h3Index());
    }

    @DeleteMapping("/subscription")
    public ResponseEntity<Void> unsubscribe(@AuthenticationPrincipal Jwt jwt) {
        ApplicationUser user = users.resolve(jwt);
        subscriptions.unsubscribe(user.id());
        return ResponseEntity.noContent().build();
    }
}
