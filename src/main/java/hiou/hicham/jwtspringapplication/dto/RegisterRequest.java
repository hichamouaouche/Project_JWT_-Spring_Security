package hiou.hicham.jwtspringapplication.dto;

public record RegisterRequest(
        String email,
        String password
) {
}
