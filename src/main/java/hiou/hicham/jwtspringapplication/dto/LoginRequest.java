package hiou.hicham.jwtspringapplication.dto;

public record LoginRequest(
        String email,
        String password
) {
}
