# JwtSpringApplication - Rapport Technique

## Description et Contexte

JwtSpringApplication est une application Spring Boot implémentant un système d'authentification basé sur JWT (JSON Web Token). Ce projet fait partie du cours de Sécurité des Systèmes Distribués (SDIA - 2ème année) à l'ENSET Mohammedia et démontre l'intégration de Spring Security avec JWT pour sécuriser une API REST.

### Objectifs Pédagogiques

Ce projet vise à :
- Comprendre le fonctionnement de l'authentification JWT
- Implémenter un système de sécurité avec Spring Security
- Maîtriser la génération et la validation de tokens
- Concevoir une architecture REST sécurisée
- Appliquer les meilleures pratiques de sécurité des applications

## Technologies Utilisées

- **Java** : 21
- **Spring Boot** : 4.1.1
- **Spring Security** : Pour la gestion de l'authentification et de l'autorisation
- **Spring Data JPA** : Pour la persistance des données
- **JWT (JJWT)** : 0.13.0 - Pour la génération et la validation de tokens
- **Base de données H2** : Base de données en mémoire pour le développement
- **Lombok** : Pour réduire le code boilerplate
- **Maven** : Gestion des dépendances

## Architecture du Projet

### Vue d'ensemble de l'Architecture

L'application suit une architecture en couches typique de Spring Boot avec une séparation claire des responsabilités :

```
┌─────────────────────────────────────────────────────────────┐
│                     Client (Frontend)                       │
│                  (Application Web / Mobile)                 │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP/REST
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                  API REST (Spring Boot)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Controllers (À Implémenter)              │  │
│  │  - AuthController (/register, /login)                │  │
│  │  - UserController (endpoints protégés)               │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │              Chaîne de Filtres de Sécurité             │  │
│  │  - JwtAuthenticationFilter (À Implémenter)           │  │
│  │  - SecurityConfiguration (À Implémenter)             │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                   Couche Services                     │  │
│  │  - JwtService (Génération/Validation JWT)            │  │
│  │  - CustomerUserDetailsService (Chargement Utilisateur)│  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                 Couche Repository                    │  │
│  │  - UserRepository (Repository JPA)                   │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                  Couche Base de Données               │  │
│  │  - Base de données H2 (En Mémoire)                   │  │
│  │  - Table : users                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Structure des Packages

```
hiou.hicham.jwtspringapplication/
├── JwtSpringApplication.java       # Classe principale de l'application (Point d'entrée)
├── dto/                            # Objets de Transfert de Données
│   ├── AuthResponse.java           # Réponse d'authentification contenant le token JWT
│   ├── LoginRequest.java           # Requête de connexion (email, mot de passe)
│   └── RegisterRequest.java        # Requête d'inscription (email, mot de passe)
├── entity/
│   └── AppUser.java                # Entité utilisateur mappée à la table "users"
├── repository/
│   └── UserRepository.java         # Interface JPA pour les opérations CRUD
└── service/
    ├── JwtService.java             # Service pour la génération et la validation de tokens JWT
    └── CustomerUserDetailsService.java # Service personnalisé pour charger les détails utilisateur
```

### Flux de Données

Le flux de données dans l'application suit le pattern MVC (Modèle-Vue-Contrôleur) adapté pour une API REST :

1. **Requête Client** → Contrôleur (Endpoint REST)
2. **Contrôleur** → Service (Logique Métier)
3. **Service** → Repository (Accès aux Données)
4. **Repository** → Base de Données (H2)
5. **Réponse** suit le chemin inverse

## Composants Principaux - Analyse Détaillée

### 1. Entité : AppUser

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/entity/AppUser.java`

L'entité `AppUser` représente un utilisateur dans le système et est mappée à la table "users" dans la base de données.

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

**Explication des Annotations** :
- `@Entity` : Indique que cette classe est une entité JPA, mappée à une table de base de données
- `@Table(name = "users")` : Spécifie le nom de la table dans la base de données
- `@Id` : Marque le champ comme clé primaire
- `@GeneratedValue(strategy = GenerationType.IDENTITY)` : Indique que l'ID est auto-généré par la base de données
- `@Column(unique = true)` : Assure que l'email est unique dans la base de données
- `@Data` (Lombok) : Génère automatiquement les getters, setters, toString, equals, hashCode
- `@AllArgsConstructor` (Lombok) : Génère un constructeur avec tous les arguments
- `@NoArgsConstructor` (Lombok) : Génère un constructeur sans arguments

**Attributs** :
- `id` : Identifiant unique auto-généré (Long)
- `email` : Adresse email de l'utilisateur (String, unique)
- `password` : Mot de passe de l'utilisateur (String, devrait être haché)
- `role` : Rôle de l'utilisateur pour la gestion de l'autorisation (String)

**Note de Sécurité** : Actuellement, le mot de passe est stocké en clair. En production, il doit être haché avec BCrypt ou un autre algorithme de hachage sécurisé.

---

### 2. DTOs (Objets de Transfert de Données)

Les DTOs sont utilisés pour transférer des données entre les couches de l'application sans exposer directement les entités.

#### 2.1 LoginRequest

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/dto/LoginRequest.java`

```java
public record LoginRequest(
        String email,
        String password
) {}
```

**Explication** :
- Utilise un `record` Java (Java 14+) pour un DTO immuable
- Contient les identifiants de connexion requis pour l'authentification
- `email` : Identifiant de l'utilisateur
- `password` : Mot de passe pour la vérification

**Utilisation** : Envoyé par le client lors d'une tentative de connexion à l'endpoint `/login`.

#### 2.2 RegisterRequest

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/dto/RegisterRequest.java`

```java
public record RegisterRequest(
        String email,
        String password
) {}
```

**Explication** :
- Similaire à LoginRequest mais utilisé pour l'inscription
- Contient les informations nécessaires pour créer un nouveau compte
- `email` : Adresse email qui servira d'identifiant unique
- `password` : Mot de passe choisi par l'utilisateur

**Utilisation** : Envoyé par le client lors de l'inscription à l'endpoint `/register`.

#### 2.3 AuthResponse

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/dto/AuthResponse.java`

```java
public record AuthResponse(
        String token) {}
```

**Explication** :
- Contient le token JWT généré après une authentification réussie
- `token` : Le JWT signé que le client doit stocker et envoyer dans les requêtes suivantes

**Utilisation** : Retourné au client après une connexion ou une inscription réussie. Le client doit inclure ce token dans l'en-tête `Authorization: Bearer <token>` pour accéder aux endpoints protégés.

---

### 3. Repository : UserRepository

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/repository/UserRepository.java`

```java
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email );
    boolean existsByEmail(String email);
}
```

**Explication** :
- `@Repository` : Indique qu'il s'agit d'un composant de la couche d'accès aux données
- `extends JpaRepository<AppUser, Long>` : Hérite des méthodes CRUD de base (save, findAll, findById, delete, etc.)
- `AppUser` : Type d'entité
- `Long` : Type d'identifiant

**Méthodes Personnalisées** :
- `Optional<AppUser> findByEmail(String email)` : Recherche un utilisateur par email. Retourne un `Optional` pour gérer le cas où l'utilisateur n'existe pas.
- `boolean existsByEmail(String email)` : Vérifie si un utilisateur avec cet email existe déjà. Utile pour éviter les doublons lors de l'inscription.

**Avantages de Spring Data JPA** :
- Aucune implémentation manuelle nécessaire, Spring génère automatiquement le SQL
- Méthodes de requête dérivées des noms de méthodes
- Support pour la pagination, le tri et les requêtes complexes

---

### 4. Services

#### 4.1 JwtService

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/service/JwtService.java`

Service responsable de la génération, de la validation et de l'extraction d'informations des tokens JWT.

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

**Explication Détaillée** :

**Configuration** :
- `@Value("${jwt.secret}")` : Injection de la clé secrète depuis application.properties
- `@Value("${jwt.expiration}")` : Injection du temps d'expiration du token en millisecondes

**Méthodes** :

1. **getSecretKey()** :
   - Décode la clé secrète encodée en Base64
   - Crée une clé HMAC-SHA pour signer les tokens
   - Utilise un algorithme de signature symétrique

2. **generateToken(UserDetails userDetails)** :
   - Génère un nouveau token JWT pour l'utilisateur
   - **subject** : Email de l'utilisateur (utilisé comme identifiant)
   - **issuedAt** : Date de génération du token
   - **expiration** : Date d'expiration (date actuelle + jwtExpiration)
   - **signWith** : Signe le token avec la clé secrète
   - **compact** : Construit et sérialise le token en String

3. **extractUsername(String token)** :
   - Extrait le sujet (email) du token
   - Utile pour identifier l'utilisateur à partir du token

4. **extractAllClaimsFromToken(String token)** :
   - Analyse et vérifie la signature du token
   - Extrait tous les claims (informations) contenus dans le token
   - Retourne l'objet Claims contenant subject, issuedAt, expiration, etc.

5. **isTokenValidForUser(String token, UserDetails userDetails)** :
   - Vérifie si le token appartient à l'utilisateur donné
   - Compare le nom d'utilisateur extrait du token avec UserDetails

6. **isTokenExpired(String token)** :
   - Vérifie si le token est expiré
   - Compare la date d'expiration avec la date actuelle

**Structure d'un Token JWT Généré** :
```
Header.Payload.Signature

Header: {"alg":"HS256","typ":"JWT"}
Payload: {"sub":"email@example.com","iat":1234567890,"exp":1234567890+3600000}
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

**Problèmes Identifiés** :
- Noms de méthodes confus (`isTokenExpired` avec deux signatures différentes)
- Aucune gestion d'exceptions lors de l'analyse du token

---

#### 4.2 CustomerUserDetailsService

**Fichier** : `src/main/java/hiou/hicham/jwtspringapplication/service/CustomerUserDetailsService.java`

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

**Explication Détaillée** :

**Interface UserDetailsService** :
- Interface Spring Security pour charger les détails de l'utilisateur
- La méthode `loadUserByUsername` est appelée automatiquement par Spring Security lors de l'authentification
- Doit retourner un objet `UserDetails` contenant les informations nécessaires pour l'authentification

**Fonctionnement** :

1. **Injection de Dépendances** :
   - `UserRepository` injecté via le constructeur
   - Pattern recommandé pour les tests et l'immuabilité

2. **loadUserByUsername(String email)** :
   - Recherche l'utilisateur dans la base de données par email
   - Si non trouvé, lance `UsernameNotFoundException`
   - Si trouvé, construit un objet Spring Security `UserDetails`

3. **Construction de UserDetails** :
   - `username` : Email de l'utilisateur (utilisé comme identifiant)
   - `password` : Mot de passe haché de l'utilisateur
   - `roles` : Rôles de l'utilisateur pour l'autorisation
   - Le `User.builder()` crée une implémentation UserDetails par défaut

**Intégration avec Spring Security** :
- Ce service sera utilisé par `DaoAuthenticationProvider`
- Spring Security l'utilise pour :
  - Vérifier les identifiants lors de la connexion
  - Charger les autorisations utilisateur (rôles)
  - Construire le contexte de sécurité (SecurityContext)

**Note** : L'annotation `@Service` est manquante sur cette classe, elle devrait être ajoutée pour que Spring puisse la détecter et l'injecter.

## Flux d'Authentification et d'Inscription

### 1. Processus d'Inscription (Register)

**Endpoint Implémenté** : `POST /auth/register`

**Description** : L'inscription permet à un nouvel utilisateur de créer un compte dans le système.

#### Étapes du Processus d'Inscription

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

#### Implémentation Actuelle (AuthController)

```java
@RestController
@RequestMapping("/auth")
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

#### Validation à Implémenter

- **Validation de l'email** : Format d'email valide
- **Validation du mot de passe** : Longueur minimale, complexité
- **Vérification d'unicité** : Email non déjà utilisé
- **Hachage** : Utiliser BCryptPasswordEncoder

---

### 2. Processus de Connexion (Login)

**Endpoint Implémenté** : `POST /auth/login`

**Description** : La connexion permet à un utilisateur existant de s'authentifier et d'obtenir un token JWT.

#### Étapes du Processus de Connexion

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

#### Implémentation Actuelle (AuthController)

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

#### Mécanisme d'Authentification Spring Security

Spring Security utilise le processus suivant :
1. **AuthenticationManager** : Coordonne le processus d'authentification
2. **DaoAuthenticationProvider** : Utilise UserDetailsService pour charger l'utilisateur
3. **PasswordEncoder** : Compare le mot de passe fourni avec le hash stocké
4. **SecurityContext** : Stocke l'Authentication si réussi

---

### 3. Accès aux Endpoints Protégés

**Description** : Une fois authentifié, le client peut accéder aux endpoints protégés en incluant le token JWT dans l'en-tête Authorization.

#### Étapes d'Accès Protégé

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐         ┌─────────────┐
│   Client    │         │  JWT Filter  │         │   Service   │         │   Database  │
└──────┬──────┘         └──────┬───────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │                       │
       │  GET /api/profile       │                       │                       │
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

#### Implémentation Actuelle (JwtFilter)

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

#### Utilisation du Token par le Client

```javascript
// Example in JavaScript (Frontend)
async function getProtectedData() {
    const token = localStorage.getItem('jwtToken');

    const response = await fetch('/api/profile', {
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

### 4. Configuration Spring Security

**Description** : La configuration Spring Security définit les règles de sécurité, les filtres, et les endpoints publics/protégés.

#### Implémentation Actuelle (SecurityConfig)

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
                .requestMatchers("/auth/**", "/h2-console/**").permitAll() // Public endpoints
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

#### Explication de la Configuration

1. **csrf.disable()** : Désactive la protection CSRF car les API REST utilisent des tokens JWT
2. **permitAll()** : Autorise l'accès sans authentification aux endpoints spécifiés
3. **authenticated()** : Nécessite une authentification pour tous les autres endpoints
4. **STATELESS** : Indique que l'application n'utilise pas de sessions HTTP
5. **addFilterBefore()** : Ajoute le filtre JWT avant le filtre d'authentification par défaut
6. **BCryptPasswordEncoder** : Encodeur de mot de passe sécurisé

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

#### Explication des Propriétés

**Base de Données H2**:
- `spring.datasource.url`: URL de connexion à la base de données H2 en mémoire
- `spring.datasource.driverClassName`: Pilote JDBC pour H2
- `spring.datasource.username`: Nom d'utilisateur H2 par défaut
- `spring.datasource.password`: Mot de passe vide par défaut

**Configuration JPA**:
- `spring.jpa.show-sql=true`: Affiche les requêtes SQL dans la console (utile pour le débogage)
- `spring.jpa.hibernate.ddl-auto=update`: Met à jour automatiquement le schéma de la base de données

**Console H2**:
- `spring.h2.console.enabled=true`: Active la console web H2
- `spring.h2.console.path=/h2-console`: Chemin d'accès à la console

**Configuration JWT**:
- `jwt.secret`: Clé secrète encodée en Base64 pour signer les tokens (DOIT être gardée secrète en production)
- `jwt.expiration=3600000`: Durée de validité du token en millisecondes (1 heure = 3600000 ms)

**Note de Sécurité** : En production, la clé secrète JWT ne doit jamais être stockée dans le code ou des fichiers de configuration versionnés. Elle devrait être injectée via des variables d'environnement ou un service de gestion des secrets.

---

### Dépendances Maven (pom.xml)

#### Dépendances Principales

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

**Explication**:
- `jjwt-api`: API pour la création et l'analyse des tokens JWT
- `jjwt-impl`: Implémentation de l'API (requise à l'exécution)
- `jjwt-jackson`: Intégration Jackson pour la sérialisation JSON

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

**Explication**:
- `spring-boot-starter-data-jpa`: Fournit JPA et Hibernate pour la persistance
- `spring-boot-starter-security`: Intégration Spring Security
- `spring-boot-starter-webmvc`: Fournit Spring MVC pour les API REST

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

### Composants Implémentés ✅

1. **Entité AppUser** : Entité utilisateur avec mappage JPA
2. **DTOs** : LoginRequest, RegisterRequest, AuthResponse
3. **UserRepository** : Repository JPA avec méthodes personnalisées
4. **JwtService** : Service complet pour la gestion des tokens JWT
5. **CustomerUserDetailsService** : Service de chargement utilisateur
6. **Configuration application.properties** : Configuration de la base de données et JWT
7. **AuthController** : Contrôleur REST pour les endpoints /register et /login
8. **UserController** : Contrôleur pour les endpoints protégés (/api/hello, /api/profile)
9. **SecurityConfig** : Configuration Spring Security
10. **JwtFilter** : Filtre pour intercepter et valider les tokens JWT
11. **PasswordEncoder** : Bean pour le hachage des mots de passe (BCrypt)

### Composants Manquants ❌

1. **Validation des DTOs** : Annotations de validation (@Valid, @Email, @Size, etc.)
2. **Gestion des Exceptions** : Gestionnaire global d'exceptions (@ControllerAdvice)
3. **Tests** : Tests unitaires et tests d'intégration
4. **Rafraîchissement de Token** : Implémentation des refresh tokens
5. **Gestion des Rôles** : Implémentation détaillée des rôles (USER, ADMIN)

## Sécurité : Analyse et Recommandations

### JWT (JSON Web Tokens)

#### Avantages
- **Stateless** : Pas besoin de stocker les sessions côté serveur
- **Scalable** : Facile à distribuer sur plusieurs serveurs
- **Standard** : Format standard (RFC 7519)
- **Flexible** : Peut contenir des claims personnalisés

#### Inconvénients et Risques
- **Non-révocable** : Une fois émis, un token est valide jusqu'à expiration
- **Vulnérable si compromis** : Si la clé secrète est compromise, tous les tokens peuvent être forgés
- **Taille** : Plus grand qu'un simple ID de session
- **Stockage côté client** : Risque XSS si stocké dans localStorage

#### Meilleures Pratiques
1. **Toujours utiliser HTTPS** : Empêche l'interception du token
2. **Expiration courte** : 15-30 minutes pour les tokens d'accès
3. **Refresh Tokens** : Implémenter des refresh tokens avec expiration plus longue
4. **Stockage sécurisé** : Préférer les cookies httpOnly au localStorage
5. **Validation stricte** : Vérifier la signature, l'expiration et l'émetteur
6. **Claims minimaux** : Stocker uniquement les informations nécessaires dans le token

### Mot de Passe

#### Problème Actuel
Les mots de passe sont stockés en clair dans la base de données, ce qui représente un risque de sécurité critique.

#### Solution Recommandée : BCrypt

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**Pourquoi BCrypt ?**
- **Adaptatif** : Le facteur de travail peut être augmenté pour compenser la puissance de calcul croissante
- **Intégré à Spring Security** : Support natif et facile à utiliser
- **Sécurisé** : Utilise un salt automatique et résiste aux attaques par rainbow table
- **Standard industriel** : Large adoption et testé

#### Exemple d'Utilisation

```java
// Hachage lors de l'inscription
String hashedPassword = passwordEncoder.encode(rawPassword);

// Vérification lors de la connexion
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

### Base de Données

#### H2 En Mémoire
- **Avantages** : Rapide, facile à configurer, pas d'installation requise
- **Inconvénients** : Données perdues au redémarrage, pas adapté pour la production

#### Recommandation pour la Production
Migrer vers une base de données persistante comme PostgreSQL ou MySQL :

```properties
# Exemple pour PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/securitydb
spring.datasource.username=postgres
spring.datasource.password=your_password
spring.datasource.driver-class-name=org.postgresql.Driver
```

### Autres Considérations de Sécurité

1. **Validation des Entrées** : Toujours valider les données utilisateur
2. **Limitation de Taux** : Limiter les tentatives de connexion pour empêcher les attaques par force brute
3. **Journalisation Sécurisée** : Ne jamais journaliser les mots de passe ou informations sensibles
4. **CORS** : Configurer correctement pour les applications frontend
5. **En-têtes de Sécurité** : Utiliser des en-têtes comme X-Content-Type-Options, X-Frame-Options

---

## Tests et Validation

### Tests Unitaires Recommandés

1. **JwtServiceTest**:
   - Tester la génération de token
   - Tester l'extraction du nom d'utilisateur
   - Tester la validation d'expiration
   - Tester avec des tokens invalides

2. **CustomerUserDetailsServiceTest**:
   - Tester le chargement d'un utilisateur existant
   - Tester le comportement avec un utilisateur inexistant
   - Tester la construction de UserDetails

3. **UserRepositoryTest**:
   - Tester la méthode findByEmail
   - Tester la méthode existsByEmail
   - Tester la création et suppression d'utilisateur

### Tests d'Intégration Recommandés

1. **AuthControllerIntegrationTest**:
   - Tester le flux d'inscription complet
   - Tester le flux de connexion complet
   - Tester les erreurs (email déjà utilisé, mauvais mot de passe)

2. **SecurityIntegrationTest**:
   - Tester que les endpoints publics sont accessibles sans token
   - Tester que les endpoints protégés nécessitent un token
   - Tester que les tokens invalides sont rejetés

### Exemple de Test (JwtServiceTest)

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

## Guide de Déploiement

### Environnement de Développement

```bash
# Cloner le dépôt
git clone <repository-url>
cd JwtSpringApplication

# Exécuter l'application
./mvnw spring-boot:run

# Accéder à l'application
# API: http://localhost:8080
# Console H2: http://localhost:8080/h2-console
```

### Configuration de Production

1. **Variables d'Environnement**:
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

3. **Build de Production**:
```bash
./mvnw clean package -DskipTests
java -jar target/JwtSpringApplication-0.0.1-SNAPSHOT.jar --spring.profiles.active=production
```

### Docker (Optionnel)

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

## Tests de l'API avec Postman

Cette section démontre comment tester l'API d'authentification JWT en utilisant Postman.

### 1. Inscription d'Utilisateur

**Endpoint** : `POST http://localhost:8080/auth/register`

**Corps de la Requête** :
```json
{
  "email": "hicham.ouaouche@gmail.com",
  "password": "1234"
}
```

**Réponse** :
```json
"Utilisateur cree"
```

![Inscription](scren/1.png)

Après une inscription réussie, l'utilisateur est stocké dans la base de données H2 :

![Base de données H2 - Table Users](scren/2.png)

### 2. Connexion d'Utilisateur

**Endpoint** : `POST http://localhost:8080/auth/login`

**Corps de la Requête** :
```json
{
  "email": "hicham.ouaouche@gmail.com",
  "password": "1234"
}
```

**Réponse** :
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJoaWNoYW0ub3VhdWNoZUBnbWFpbC5jb20iLCJpYXQiOjE3OTIwODYwMjEsImV4cCI6MTc5MjE3MjQyMX0.f63zsBY40QERIzVJojcxOCISPV30x1PLz2IGh16bYms"
}
```

![Connexion](scren/3.png)

### 3. Accès à l'Endpoint Profile

**Endpoint** : `GET http://localhost:8080/user/profile`

**En-têtes** :
```
Authorization: Bearer <votre-token-jwt>
```

![Profile](scren/4.png)

### 4. Accès à l'Endpoint Hello

**Endpoint** : `GET http://localhost:8080/hello`

**En-têtes** :
```
Authorization: Bearer <votre-token-jwt>
```

![Hello](scren/5.png)

---

## Conclusion

JwtSpringApplication est un projet éducatif démontrant l'implémentation d'un système d'authentification JWT avec Spring Boot. Bien que les fondations soient solides, le projet nécessite encore plusieurs composants pour être fonctionnel et sécurisé en production.

### Points Forts

- ✅ Architecture claire et bien structurée
- ✅ Utilisation de standards (JWT, Spring Security, JPA)
- ✅ Code lisible et maintenable
- ✅ Services JWT bien implémentés

### Zones d'Amélioration

- ❌ Contrôleurs REST manquants
- ❌ Configuration Spring Security incomplète
- ❌ Aucun hachage de mot de passe
- ❌ Bugs mineurs dans le code existant
- ❌ Aucun test

### Recommandations

1. **Prioriser la sécurité** : Implémenter BCrypt pour les mots de passe avant toute autre fonctionnalité
2. **Compléter l'authentification** : Implémenter AuthController et SecurityConfiguration
3. **Ajouter des tests** : Écrire des tests unitaires et d'intégration.

Ce projet fournit une excellente base pour comprendre l'authentification JWT avec Spring Security et peut être étendu pour devenir une application de production complète et sécurisée.

---

## Auteur

**Hicham Ouaouche**

Projet réalisé dans le cadre du cours de Sécurité des Systèmes Distribués (SDIA - 2ème année) à l'ENSET Mohammedia.

