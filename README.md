# JwtSpringApplication - Technical Report

## Description and Context

JwtSpringApplication is a Spring Boot application implementing a JWT (JSON Web Token) based authentication system. This project is part of the Distributed Systems Security course (SDIA - 2nd Year) at ENSET Mohammedia and demonstrates the integration of Spring Security with JWT to secure a REST API.

### Educational Objectives

This project aims to:
- Understand how JWT authentication works
- Implement a security system with Spring Security
- Master token generation and validation
- Design a secure REST architecture
- Apply application security best practices

## Technologies Used

- **Java**: 21
- **Spring Boot**: 4.1.1
- **Spring Security**: For authentication and authorization management
- **Spring Data JPA**: For data persistence
- **JWT (JJWT)**: 0.13.0 - For token generation and validation
- **H2 Database**: In-memory database for development
- **Lombok**: To reduce boilerplate code
- **Maven**: Dependency management

## Project Architecture

### Architecture Overview

The application follows a typical Spring Boot layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Frontend)                       │
│                  (Web Application / Mobile)                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  API REST (Spring Boot)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Controllers (To Implement)               │  │
│  │  - AuthController (/register, /login)                │  │
│  │  - UserController (protected endpoints)              │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │              Security Filter Chain                    │  │
│  │  - JwtAuthenticationFilter (To Implement)            │  │
│  │  - SecurityConfiguration (To Implement)              │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                   Services Layer                       │  │
│  │  - JwtService (JWT Generation/Validation)            │  │
│  │  - CustomerUserDetailsService (User Loading)          │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                 Repository Layer                       │  │
│  │  - UserRepository (JPA Repository)                    │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                  Database Layer                        │  │
│  │  - H2 Database (In-Memory)                             │  │
│  │  - Table: users                                        │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Package Structure

```
hiou.hicham.jwtspringapplication/
├── JwtSpringApplication.java       # Main application class (Entry point)
├── dto/                            # Data Transfer Objects
│   ├── AuthResponse.java           # Authentication response containing JWT token
│   ├── LoginRequest.java           # Login request (email, password)
│   └── RegisterRequest.java        # Registration request (email, password)
├── entity/
│   └── AppUser.java                # User entity mapped to "users" table
├── repository/
│   └── UserRepository.java         # JPA interface for CRUD operations
└── service/
    ├── JwtService.java             # Service for JWT token generation and validation
    └── CustomerUserDetailsService.java # Custom service to load user details
```

### Data Flow

The data flow in the application follows the MVC (Model-View-Controller) pattern adapted for a REST API:

1. **Client Request** → Controller (REST Endpoint)
2. **Controller** → Service (Business Logic)
3. **Service** → Repository (Data Access)
4. **Repository** → Database (H2)
5. **Response** follows the reverse path

## Main Components - Detailed Analysis

### 1. Entity: AppUser

**File**: `src/main/java/hiou/hicham/jwtspringapplication/entity/AppUser.java`

The `AppUser` entity represents a user in the system and is mapped to the "users" table in the database.

```java
@Entity
@Table(name = "users")
@Data
@AllArgsConstructor @NoArgsConstructor
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String password;
    @Column(unique = true)
    private String email;
    private String role;
}
```

**Annotations Explained** :
- `@Entity`: Indicates that this class is a JPA entity, mapped to a database table
- `@Table(name = "users")`: Specifies the table name in the database
- `@Id`: Marks the field as primary key
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Indicates that the ID is auto-generated by the database
- `@Column(unique = true)`: Ensures that the email is unique in the database
- `@Data` (Lombok): Automatically generates getters, setters, toString, equals, hashCode
- `@AllArgsConstructor` (Lombok): Generates a constructor with all arguments
- `@NoArgsConstructor` (Lombok): Generates a no-argument constructor

**Attributes** :
- `id`: Unique auto-generated identifier (Long)
- `email`: User email address (String, unique)
- `password`: User password (String, should be hashed)
- `role`: User role for authorization management (String)

**Security Note**: Currently, the password is stored in plain text. In production, it must be hashed with BCrypt or another secure hashing algorithm.

---

### 2. DTOs (Data Transfer Objects)

DTOs are used to transfer data between application layers without directly exposing entities.

#### 2.1 LoginRequest

**File**: `src/main/java/hiou/hicham/jwtspringapplication/dto/LoginRequest.java`

```java
public record LoginRequest(
        String email,
        String password
) {}
```

**Explanation** :
- Uses a Java `record` (Java 14+) for an immutable DTO
- Contains login credentials required for authentication
- `email`: User identifier
- `password`: Password for verification

**Usage**: Sent by the client during a login attempt to the `/login` endpoint.

#### 2.2 RegisterRequest

**File**: `src/main/java/hiou/hicham/jwtspringapplication/dto/RegisterRequest.java`

```java
public record RegisterRequest(
        String email,
        String password
) {}
```

**Explanation** :
- Similar to LoginRequest but used for registration
- Contains information needed to create a new account
- `email`: Email address that will serve as unique identifier
- `password`: Password chosen by the user

**Usage**: Sent by the client during registration to the `/register` endpoint.

#### 2.3 AuthResponse

**File**: `src/main/java/hiou/hicham/jwtspringapplication/dto/AuthResponse.java`

```java
public record AuthResponse(
        String token) {}
```

**Explanation** :
- Contains the JWT token generated after successful authentication
- `token`: The signed JWT that the client must store and send in subsequent requests

**Usage**: Returned to the client after successful login or registration. The client must include this token in the `Authorization: Bearer <token>` header to access protected endpoints.

---

### 3. Repository: UserRepository

**File**: `src/main/java/hiou/hicham/jwtspringapplication/repository/UserRepository.java`

```java
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email );
    boolean existsByEmail(String email);
}
```

**Explanation** :
- `@Repository`: Indicates that this is a data access layer component
- `extends JpaRepository<AppUser, Long>`: Inherits basic CRUD methods (save, findAll, findById, delete, etc.)
- `AppUser`: Entity type
- `Long`: Identifier type

**Custom Methods** :
- `Optional<AppUser> findByEmail(String email)`: Searches for a user by email. Returns an `Optional` to handle the case where the user doesn't exist.
- `boolean existsByEmail(String email)`: Checks if a user with this email already exists. Useful to avoid duplicates during registration.

**Advantages of Spring Data JPA** :
- No manual implementation needed, Spring automatically generates SQL
- Query methods derived from method names
- Support for pagination, sorting, and complex queries

---

### 4. Services

#### 4.1 JwtService

**File**: `src/main/java/hiou/hicham/jwtspringapplication/service/JwtService.java`

Service responsible for JWT token generation, validation, and information extraction.

```java
@Service
public class JwtService {
    @Value("jwt.secret")
    private String jwtSecret;
    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis()+jwtExpiration))
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String token){
        return extractAllClaimsFromToken(token).getSubject();
    }

    public Claims extractAllClaimsFromToken(String token){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token, UserDetails userDetails){
        String userName = extractUsername(token);
        return userDetails.getUsername().equals(userName);
    }

    public boolean isTokenExpired(String token){
        Date expiration = extractAllClaimsFromToken(token).getExpiration();
        return expiration.before(new Date());
    }
}
```

**Detailed Explanation** :

**Configuration** :
- `@Value("jwt.secret")`: Injection of the secret key from application.properties (note: there is a bug here, should be `${jwt.secret}`)
- `@Value("${jwt.expiration}")`: Injection of the token expiration time in milliseconds

**Methods** :

1. **getSecretKey()**:
   - Decodes the Base64-encoded secret key
   - Creates an HMAC-SHA key to sign tokens
   - Uses symmetric signature algorithm

2. **generateToken(UserDetails userDetails)**:
   - Generates a new JWT token for the user
   - **subject**: User's email (used as identifier)
   - **issuedAt**: Token generation date
   - **expiration**: Expiration date (current date + jwtExpiration)
   - **signWith**: Signs the token with the secret key
   - **compact**: Builds and serializes the token to String

3. **extractUsername(String token)**:
   - Extracts the subject (email) from the token
   - Useful to identify the user from the token

4. **extractAllClaimsFromToken(String token)**:
   - Parses and verifies the token signature
   - Extracts all claims (information) contained in the token
   - Returns the Claims object containing subject, issuedAt, expiration, etc.

5. **isTokenExpired(String token, UserDetails userDetails)**:
   - Checks if the token belongs to the given user
   - Compares the username extracted from the token with UserDetails
   - **Note**: The method name is misleading, it should be called `isTokenValidForUser`

6. **isTokenExpired(String token)**:
   - Checks if the token is expired
   - Compares the expiration date with the current date
   - **Note**: The method name is misleading, it should be called `isTokenExpired` (returns true if expired)

**Structure of a Generated JWT Token** :
```
Header.Payload.Signature

Header: {"alg":"HS256","typ":"JWT"}
Payload: {"sub":"email@example.com","iat":1234567890,"exp":1234567890+3600000}
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

**Identified Issues** :
- Line 20: `@Value("jwt.secret")` should be `@Value("${jwt.secret}")` to read the property correctly
- Confusing method names (`isTokenExpired` with two different signatures)
- No exception handling during token parsing

---

#### 4.2 CustomerUserDetailsService

**File**: `src/main/java/hiou/hicham/jwtspringapplication/service/CustomerUserDetailsService.java`

```java
public class CustomerUserDetailsService implements UserDetailsService {
    private final  UserRepository userRepository;

    public CustomerUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email not found"));

        return User.builder()
                .username(appUser.getEmail())
                .password(appUser.getPassword())
                .roles(appUser.getRole())
                .build();
    }
}
```

**Detailed Explanation** :

**UserDetailsService Interface** :
- Spring Security interface for loading user details
- `loadUserByUsername` method is automatically called by Spring Security during authentication
- Must return a `UserDetails` object containing information needed for authentication

**How It Works** :

1. **Dependency Injection** :
   - `UserRepository` injected via constructor
   - Recommended pattern for testing and immutability

2. **loadUserByUsername(String email)** :
   - Searches for the user in the database by email
   - If not found, throws `UsernameNotFoundException`
   - If found, builds a Spring Security `UserDetails` object

3. **Building UserDetails** :
   - `username`: User's email (used as identifier)
   - `password`: User's hashed password
   - `roles`: User's roles for authorization
   - The `User.builder()` creates a default UserDetails implementation

**Integration with Spring Security** :
- This service will be used by `DaoAuthenticationProvider`
- Spring Security uses it to:
  - Verify credentials during login
  - Load user authorizations (roles)
  - Build the security context (SecurityContext)

**Note**: The `@Service` annotation is missing on this class, it should be added so Spring can detect and inject it.

## Authentication and Registration Flow

### 1. Registration Process (Register)

**Planned Endpoint**: `POST /api/auth/register`

**Description**: Registration allows a new user to create an account in the system.

#### Registration Process Steps

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │         │  Controller  │         │   Service   │         │   Database  │
└──────┬──────┘         └──────┬───────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │                       │
       │  POST /register       │                       │                       │
       │  {email, password}    │                       │                       │
       │──────────────────────>│                       │                       │
       │                       │                       │                       │
       │                       │  Validate DTO         │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Check if email       │                       │
       │                       │  already exists       │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │                       │
       │                       │  existsByEmail()       │                       │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  If exists: Error     │                       │
       │                       │  Else: Continue       │                       │
       │                       │                       │                       │
       │                       │  Hash password        │                       │
       │                       │  (BCrypt)             │                       │
       │                       │                       │                       │
       │                       │  Create AppUser       │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │    INSERT INTO users  │
       │                       │                       │                       │
       │                       │  Generate JWT Token   │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Return AuthResponse  │                       │
       │                       │  {token}              │                       │
       │<──────────────────────│                       │                       │
       │                       │                       │                       │
       │  Store token           │                       │                       │
       │  (localStorage/cookie) │                       │                       │
```

#### Planned Implementation (AuthController)

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // 1. Check if email already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already in use");
        }

        // 2. Hash the password
        String hashedPassword = passwordEncoder.encode(request.password());

        // 3. Create the user
        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPassword(hashedPassword);
        user.setRole("USER"); // Default role

        userRepository.save(user);

        // 4. Generate JWT token
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        // 5. Return response with token
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
```

#### Validation to Implement

- **Email validation**: Valid email format
- **Password validation**: Minimum length, complexity
- **Uniqueness check**: Email not already used
- **Hashing**: Use BCryptPasswordEncoder

---

### 2. Login Process

**Planned Endpoint**: `POST /api/auth/login`

**Description**: Login allows an existing user to authenticate and obtain a JWT token.

#### Login Process Steps

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │         │  Controller  │         │   Service   │         │   Database  │
└──────┬──────┘         └──────┬───────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │                       │
       │  POST /login          │                       │                       │
       │  {email, password}    │                       │                       │
       │──────────────────────>│                       │                       │
       │                       │                       │                       │
       │                       │  Authenticate via     │                       │
       │                       │  AuthenticationManager│                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  loadUserByUsername() │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │  SELECT * FROM users  │
       │                       │                       │  WHERE email = ?      │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  Verify password       │                       │
       │                       │  (BCrypt check)        │                       │
       │                       │                       │                       │
       │                       │  If OK: Generate Token │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  generateToken()      │                       │
       │                       │<──────────────────────│                       │
       │                       │                       │                       │
       │                       │  Return AuthResponse  │                       │
       │  {token}              │                       │                       │
       │<──────────────────────│                       │                       │
       │                       │                       │                       │
       │  Store token           │                       │                       │
```

#### Planned Implementation (AuthController)

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    // 1. Authenticate user via AuthenticationManager
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.email(),
            request.password()
        )
    );

    // 2. If authentication successful, load user
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

    // 3. Generate JWT token
    String token = jwtService.generateToken(userDetails);

    // 4. Return response with token
    return ResponseEntity.ok(new AuthResponse(token));
}
```

#### Spring Security Authentication Mechanism

Spring Security uses the following process:
1. **AuthenticationManager**: Coordinates the authentication process
2. **DaoAuthenticationProvider**: Uses UserDetailsService to load the user
3. **PasswordEncoder**: Compares the provided password with the stored hash
4. **SecurityContext**: Stores the Authentication if successful

---

### 3. Access to Protected Endpoints

**Description**: Once authenticated, the client can access protected endpoints by including the JWT token in the Authorization header.

#### Protected Access Steps

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │         │  JWT Filter  │         │   Service   │         │   Database  │
└──────┬──────┘         └──────┬───────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │                       │
       │  GET /api/user/profile│                       │                       │
       │  Authorization:       │                       │                       │
       │  Bearer <token>        │                       │                       │
       │──────────────────────>│                       │                       │
       │                       │                       │                       │
       │                       │  Extract token from    │                       │
       │                       │  Authorization header  │                       │
       │                       │                       │                       │
       │                       │  Validate token        │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  extractUsername()     │                       │
       │                       │<──────────────────────│                       │
       │                       │                       │                       │
       │                       │  Load user             │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │                       │
       │                       │  loadUserByUsername()  │                       │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  Create Authentication │                       │
       │                       │  and SecurityContext   │                       │
       │                       │                       │                       │
       │                       │  Let request pass to   │                       │
       │                       │  Controller           │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Process request       │                       │
       │  Response             │                       │                       │
       │<──────────────────────│                       │                       │
```

#### Planned Implementation (JwtAuthenticationFilter)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extract token from Authorization header
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtService.extractUsername(token);
        }

        // 2. If token present and user not authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 3. Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 4. Validate token
            if (jwtService.isTokenExpired(token, userDetails) && !jwtService.isTokenExpired(token)) {
                // 5. Create Authentication object
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );

                // 6. Set security context
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 7. Continue filter chain
        filterChain.doFilter(request, response);
    }
}
```

#### Client Token Usage

```javascript
// Example in JavaScript (Frontend)
async function getProtectedData() {
    const token = localStorage.getItem('jwtToken');

    const response = await fetch('/api/user/profile', {
        method: 'GET',
        headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        }
    });

    const data = await response.json();
    return data;
}
```

---

### 4. Spring Security Configuration

**Description**: Spring Security configuration defines security rules, filters, and public/protected endpoints.

#### Planned Implementation (SecurityConfiguration)

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomerUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for REST API
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll() // Public endpoints
                .requestMatchers("/h2-console/**").permitAll() // Public H2 console
                .anyRequest().authenticated() // All other endpoints protected
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // No HTTP session
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Add JWT filter

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
}
```

#### Configuration Explanation

1. **csrf.disable()**: Disables CSRF protection as REST APIs use JWT tokens
2. **permitAll()**: Allows access without authentication to specified endpoints
3. **authenticated()**: Requires authentication for all other endpoints
4. **STATELESS**: Indicates that the application doesn't use HTTP sessions
5. **addFilterBefore()**: Adds JWT filter before the default authentication filter
6. **BCryptPasswordEncoder**: Secure password encoder

---

## Configuration

### application.properties

```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:securitydbdemo
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA Configuration
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# JWT Configuration
jwt.secret=TXlKV1RTZWNyZXRIaWNoYW1PdWFvdWNoZWhkaGFoaGNIR0d3VVVWQkhZR1NIWVhVVUNCRw==
jwt.expiration=3600000
```

#### Property Explanation

**H2 Database**:
- `spring.datasource.url`: H2 in-memory database connection URL
- `spring.datasource.driverClassName`: JDBC driver for H2
- `spring.datasource.username`: Default H2 username
- `spring.datasource.password`: Default empty password

**JPA Configuration**:
- `spring.jpa.show-sql=true`: Displays SQL queries in console (useful for debugging)
- `spring.jpa.hibernate.ddl-auto=update`: Automatically updates database schema

**H2 Console**:
- `spring.h2.console.enabled=true`: Enables H2 web console
- `spring.h2.console.path=/h2-console`: Console access path

**JWT Configuration**:
- `jwt.secret`: Base64-encoded secret key for signing tokens (MUST be kept secret in production)
- `jwt.expiration=3600000`: Token validity duration in milliseconds (1 hour = 3600000 ms)

**Security Note**: In production, the JWT secret key should never be stored in code or versioned configuration files. It should be injected via environment variables or a secret management service.

---

### Maven Dependencies (pom.xml)

#### Main Dependencies

```xml
<!-- JWT (JJWT) -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

**Explanation**:
- `jjwt-api`: API for JWT token creation and parsing
- `jjwt-impl`: API implementation (required at runtime)
- `jjwt-jackson`: Jackson integration for JSON serialization

#### Spring Boot Starters

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

**Explanation**:
- `spring-boot-starter-data-jpa`: Provides JPA and Hibernate for persistence
- `spring-boot-starter-security`: Spring Security integration
- `spring-boot-starter-webmvc`: Provides Spring MVC for REST APIs

#### Database

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Explanation**:
- H2 in-memory database for development and testing
- Scope `runtime` as it's only needed at execution

#### Utilities

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Explanation**:
- Lombok reduces boilerplate code (getters, setters, constructors)
- Annotation processor configured in maven-compiler-plugin

---

## Current Project Status

### Implemented Components ✅

1. **Entity AppUser**: User entity with JPA mapping
2. **DTOs**: LoginRequest, RegisterRequest, AuthResponse
3. **UserRepository**: JPA repository with custom methods
4. **JwtService**: Complete service for JWT token management
5. **CustomerUserDetailsService**: User loading service
6. **Configuration application.properties**: Database and JWT configuration

### Missing Components ❌

1. **AuthController**: REST controller for /register and /login endpoints
2. **UserController**: Controller for protected endpoints
3. **SecurityConfiguration**: Spring Security configuration
4. **JwtAuthenticationFilter**: Filter to intercept and validate JWT tokens
5. **PasswordEncoder**: Bean for password hashing (BCrypt)
6. **DTO Validation**: Validation annotations (@Valid, @Email, @Size, etc.)
7. **Exception Handling**: Global exception handler
8. **Tests**: Unit and integration tests

### Identified Issues in Existing Code ⚠️

1. **JwtService.java - Line 20**:
   ```java
   @Value("jwt.secret")  // ❌ Incorrect
   ```
   Should be:
   ```java
   @Value("${jwt.secret}")  // ✅ Correct
   ```

2. **CustomerUserDetailsService.java**:
   - Missing `@Service` annotation
   - Spring won't be able to detect and inject this bean automatically

3. **Confusing method names in JwtService**:
   - `isTokenExpired(String token, UserDetails)`: Checks if token belongs to user (not expiration)
   - `isTokenExpired(String token)`: Checks token expiration
   - Should be renamed for clarity

4. **No password hashing**:
   - Passwords are stored in plain text
   - Critical security risk

5. **No exception handling**:
   - No try-catch during token parsing
   - Risk of unhandled exceptions in production

---

## Security: Analysis and Recommendations

### JWT (JSON Web Tokens)

#### Advantages
- **Stateless**: No need to store sessions on server side
- **Scalable**: Easy to distribute across multiple servers
- **Standard**: Standard format (RFC 7519)
- **Flexible**: Can contain custom claims

#### Disadvantages and Risks
- **Non-revocable**: Once issued, a token is valid until expiration
- **Vulnerable if compromised**: If secret key is compromised, all tokens can be forged
- **Size**: Larger than a simple session ID
- **Client-side storage**: XSS risk if stored in localStorage

#### Best Practices
1. **Always use HTTPS**: Prevent token interception
2. **Short expiration**: 15-30 minutes for access tokens
3. **Refresh Tokens**: Implement refresh tokens with longer expiration
4. **Secure storage**: Prefer httpOnly cookies over localStorage
5. **Strict validation**: Verify signature, expiration, and issuer
6. **Minimal claims**: Store only necessary information in token

### Password

#### Current Problem
Passwords are stored in plain text in the database, which represents a critical security risk.

#### Recommended Solution: BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Why BCrypt?**
- **Adaptive**: Work factor can be increased to compensate for increasing computing power
- **Built into Spring Security**: Native support and easy to use
- **Secure**: Uses automatic salt and is resistant to rainbow table attacks
- **Industry standard**: Widely adopted and tested

#### Usage Example

```java
// Hashing during registration
String hashedPassword = passwordEncoder.encode(rawPassword);

// Verification during login
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

### Database

#### H2 In-Memory
- **Advantages**: Fast, easy to configure, no installation required
- **Disadvantages**: Data lost on restart, not suitable for production

#### Production Recommendation
Migrate to a persistent database like PostgreSQL or MySQL:

```properties
# Example for PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/securitydb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Other Security Considerations

1. **Input Validation**: Always validate user data
2. **Rate Limiting**: Limit login attempts to prevent brute force attacks
3. **Secure Logging**: Never log passwords or sensitive information
4. **CORS**: Configure correctly for frontend applications
5. **Security Headers**: Use headers like X-Content-Type-Options, X-Frame-Options

---

## Testing and Validation

### Recommended Unit Tests

1. **JwtServiceTest**:
   - Test token generation
   - Test username extraction
   - Test expiration validation
   - Test with invalid tokens

2. **CustomerUserDetailsServiceTest**:
   - Test loading an existing user
   - Test behavior with non-existent user
   - Test UserDetails construction

3. **UserRepositoryTest**:
   - Test findByEmail method
   - Test existsByEmail method
   - Test user creation and deletion

### Recommended Integration Tests

1. **AuthControllerIntegrationTest**:
   - Test complete registration flow
   - Test complete login flow
   - Test errors (email already used, wrong password)

2. **SecurityIntegrationTest**:
   - Test that public endpoints are accessible without token
   - Test that protected endpoints require a token
   - Test that invalid tokens are rejected

### Test Example (JwtServiceTest)

```java
@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldGenerateToken() {
        UserDetails userDetails = User.builder()
            .username("test@example.com")
            .password("password")
            .roles("USER")
            .build();

        String token = jwtService.generateToken(userDetails);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void shouldExtractUsername() {
        UserDetails userDetails = User.builder()
            .username("test@example.com")
            .password("password")
            .roles("USER")
            .build();

        String token = jwtService.generateToken(userDetails);
        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("test@example.com", extractedUsername);
    }
}
```

---

## Deployment Guide

### Development Environment

```bash
# Clone the repository
git clone <repository-url>
cd JwtSpringApplication

# Run the application
./mvnw spring-boot:run

# Access the application
# API: http://localhost:8080
# H2 Console: http://localhost:8080/h2-console
```

### Production Configuration

1. **Environment Variables**:
```bash
export JWT_SECRET=your_very_long_and_random_secret_key
export JWT_EXPIRATION=3600000
export DB_URL=jdbc:postgresql://your-db:5432/securitydb
export DB_USERNAME=your_username
export DB_PASSWORD=your_password
```

2. **application-production.properties**:
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}
```

3. **Production Build**:
```bash
./mvnw clean package -DskipTests
java -jar target/JwtSpringApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

### Docker (Optional)

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/JwtSpringApplication-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t jwt-spring-app .
docker run -p 8080:8080 jwt-spring-app
```

---

## Future Improvements

### Short Term (High Priority)

- [ ] **Fix identified bugs**:
  - Fix `@Value("jwt.secret")` to `@Value("${jwt.secret}")`
  - Add `@Service` on CustomerUserDetailsService
  - Rename confusing methods in JwtService

- [ ] **Implement AuthController**:
  - POST /api/auth/register endpoint
  - POST /api/auth/login endpoint
  - DTO validation

- [ ] **Implement SecurityConfiguration**:
  - SecurityFilterChain configuration
  - PasswordEncoder bean (BCrypt)
  - AuthenticationManager bean

- [ ] **Implement JwtAuthenticationFilter**:
  - Token extraction from Authorization header
  - Token validation
  - SecurityContext creation

- [ ] **Add password hashing**:
  - Use BCryptPasswordEncoder
  - Hash during registration
  - Verify during login

### Medium Term (Medium Priority)

- [ ] **Implement UserController**:
  - GET /api/user/profile endpoint (protected)
  - PUT /api/user/profile endpoint (protected)
  - DELETE /api/user/account endpoint (protected)

- [ ] **Role Management**:
  - Implement different roles (USER, ADMIN)
  - Role-specific endpoints
  - Role-based authorizations

- [ ] **Refresh Token**:
  - Implement refresh tokens
  - POST /api/auth/refresh endpoint
  - Token rotation

- [ ] **DTO Validation**:
  - Add @Valid on parameters
  - @Email, @Size, @NotBlank annotations
  - Custom error messages

- [ ] **Exception Handling**:
  - Global @ControllerAdvice
  - Custom exceptions
  - Structured error responses

### Long Term (Low Priority)

- [ ] **Migrate to persistent database**:
  - PostgreSQL or MySQL
  - Production configuration
  - Database migrations (Flyway/Liquibase)

- [ ] **Complete Testing**:
  - Unit tests for all services
  - Integration tests for controllers
  - Security tests

- [ ] **Rate Limiting**:
  - Limit login attempts
  - Limit requests per IP
  - Use Spring Boot Starter for Resilience4j

- [ ] **Audit Logging**:
  - Log successful/failed logins
  - Log sensitive actions
  - GDPR compliance

- [ ] **OAuth2 / OpenID Connect**:
  - Third-party authentication support (Google, GitHub)
  - Integration with identity providers

- [ ] **2FA (Two-Factor Authentication)**:
  - Implement TOTP (Time-based One-Time Password)
  - SMS or Email for verification code

---

## API Testing with Postman

This section demonstrates how to test the JWT authentication API using Postman.

### 1. User Registration

**Endpoint**: `POST http://localhost:8080/auth/register`

**Request Body**:
```json
{
  "email": "hicham.ouaouche@gmail.com",
  "password": "1234"
}
```

**Response**:
```json
"Utilisateur cree"
```

![Registration](scren/1.png)

After successful registration, the user is stored in the H2 database:

![H2 Database - Users Table](scren/2.png)

### 2. User Login

**Endpoint**: `POST http://localhost:8080/auth/login`

**Request Body**:
```json
{
  "email": "hicham.ouaouche@gmail.com",
  "password": "1234"
}
```

**Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoaWNoYW0ub3VhdWNoZUBnbWFpbC5jb20iLCJpYXQiOjE3OTIwODYwMjEsImV4cCI6MTc5MjE3MjQyMX0.f63zsBY40QERIzVJojcxOCISPV30x1PLz2IGh16bYms"
}
```

![Login](scren/3.png)

### 3. Access Profile Endpoint

**Endpoint**: `GET http://localhost:8080/user/profile`

**Headers**:
```
Authorization: Bearer <your-jwt-token>
```

![Profile](scren/4.png)

### 4. Access Hello Endpoint

**Endpoint**: `GET http://localhost:8080/hello`

**Headers**:
```
Authorization: Bearer <your-jwt-token>
```

![Hello](scren/5.png)

---

## Conclusion

JwtSpringApplication is an educational project demonstrating the implementation of a JWT authentication system with Spring Boot. While the foundations are solid, the project still requires several components to be functional and secure in production.

### Strengths

- ✅ Clear and well-structured architecture
- ✅ Use of standards (JWT, Spring Security, JPA)
- ✅ Readable and maintainable code
- ✅ Well-implemented JWT services

### Areas for Improvement

- ❌ Missing REST controllers
- ❌ Incomplete Spring Security configuration
- ❌ No password hashing
- ❌ Minor bugs in existing code
- ❌ No tests

### Recommendations

1. **Prioritize security**: Implement BCrypt for passwords before any other feature
2. **Complete authentication**: Implement AuthController and SecurityConfiguration
3. **Add tests**: Write unit and integration tests
4. **Fix bugs**: Resolve identified issues in existing code
5. **Document API**: Use Swagger/OpenAPI to document endpoints

This project provides an excellent foundation for understanding JWT authentication with Spring Security and can be extended to become a complete and secure production application.

---

## Author

**Hicham Ouaouche**

Project completed as part of the Distributed Systems Security course (SDIA - 2nd Year) at ENSET Mohammedia.

