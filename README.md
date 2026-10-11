# JwtSpringApplication - Rapport Technique

## Description et Contexte

JwtSpringApplication est une application Spring Boot implémentant un système d'authentification basé sur JSON Web Tokens (JWT). Ce projet fait partie du cours de sécurité des applications (SDIA - 2 ans) et démontre l'intégration de Spring Security avec JWT pour sécuriser une API REST.

### Objectifs Pédagogiques

Ce projet vise à :
- Comprendre le fonctionnement de l'authentification par JWT
- Implémenter un système de sécurité avec Spring Security
- Maîtriser la génération et la validation des tokens
- Concevoir une architecture REST sécurisée
- Appliquer les bonnes pratiques de sécurité des applications

## Technologies Utilisées

- **Java**: 21
- **Spring Boot**: 4.1.1
- **Spring Security**: Pour la gestion de l'authentification et de l'autorisation
- **Spring Data JPA**: Pour la persistance des données
- **JWT (JJWT)**: 0.13.0 - Pour la génération et la validation des tokens
- **H2 Database**: Base de données en mémoire pour le développement
- **Lombok**: Pour réduire le code boilerplate
- **Maven**: Gestion des dépendances

## Architecture du Projet

### Vue d'Ensemble de l'Architecture

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
│  │  - UserController (endpoints protégés)                │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │              Security Filter Chain                    │  │
│  │  - JwtAuthenticationFilter (À Implémenter)           │  │
│  │  - SecurityConfiguration (À Implémenter)             │  │
│  └──────────────────────┬───────────────────────────────┘  │
│                         │                                    │
│  ┌──────────────────────▼───────────────────────────────┐  │
│  │                   Services Layer                       │  │
│  │  - JwtService (Génération/Validation JWT)             │  │
│  │  - CustomerUserDetailsService (Chargement utilisateur)│  │
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

### Structure des Packages

```
hiou.hicham.jwtspringapplication/
├── JwtSpringApplication.java       # Classe principale de l'application (Point d'entrée)
├── dto/                            # Data Transfer Objects
│   ├── AuthResponse.java           # Réponse d'authentification contenant le token JWT
│   ├── LoginRequest.java           # Requête de connexion (email, password)
│   └── RegisterRequest.java        # Requête d'inscription (email, password)
├── entity/
│   └── AppUser.java                # Entité utilisateur mappée à la table "users"
├── repository/
│   └── UserRepository.java         # Interface JPA pour les opérations CRUD
└── service/
    ├── JwtService.java             # Service pour la génération et validation des tokens JWT
    └── CustomerUserDetailsService.java # Service personnalisé pour charger les détails utilisateur
```

### Flux de Données

Le flux de données dans l'application suit le pattern MVC (Model-View-Controller) adapté pour une API REST :

1. **Requête Client** → Controller (REST Endpoint)
2. **Controller** → Service (Logique métier)
3. **Service** → Repository (Accès données)
4. **Repository** → Base de données (H2)
5. **Réponse** suit le chemin inverse

## Composants Principaux - Analyse Détaillée

### 1. Entity: AppUser

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/entity/AppUser.java`

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

**Annotations Expliquées** :
- `@Entity`: Indique que cette classe est une entité JPA, mappée à une table de base de données
- `@Table(name = "users")`: Spécifie le nom de la table dans la base de données
- `@Id`: Marque le champ comme clé primaire
- `@GeneratedValue(strategy = GenerationType.IDENTITY)`: Indique que l'ID est auto-généré par la base de données
- `@Column(unique = true)`: Assure que l'email est unique dans la base de données
- `@Data` (Lombok): Génère automatiquement getters, setters, toString, equals, hashCode
- `@AllArgsConstructor` (Lombok): Génère un constructeur avec tous les arguments
- `@NoArgsConstructor` (Lombok): Génère un constructeur sans arguments

**Attributs** :
- `id`: Identifiant unique auto-généré (Long)
- `email`: Adresse email de l'utilisateur (String, unique)
- `password`: Mot de passe de l'utilisateur (String, devrait être hashé)
- `role`: Rôle de l'utilisateur pour la gestion des autorisations (String)

**Note de Sécurité** : Actuellement, le mot de passe est stocké en clair. En production, il doit être hashé avec BCrypt ou un autre algorithme de hashage sécurisé.

---

### 2. DTOs (Data Transfer Objects)

Les DTOs servent à transférer des données entre les couches de l'application sans exposer directement les entités.

#### 2.1 LoginRequest

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/dto/LoginRequest.java`

```java
public record LoginRequest(
        String email,
        String password
) {}
```

**Explication** :
- Utilise un `record` Java (Java 14+) pour un DTO immuable
- Contient les identifiants de connexion nécessaires à l'authentification
- `email`: Identifiant de l'utilisateur
- `password`: Mot de passe pour vérification

**Utilisation** : Envoyé par le client lors d'une tentative de connexion au endpoint `/login`.

#### 2.2 RegisterRequest

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/dto/RegisterRequest.java`

```java
public record RegisterRequest(
        String email,
        String password
) {}
```

**Explication** :
- Similaire à LoginRequest mais utilisé pour l'inscription
- Contient les informations nécessaires pour créer un nouveau compte
- `email`: Adresse email qui servira d'identifiant unique
- `password`: Mot de passe choisi par l'utilisateur

**Utilisation** : Envoyé par le client lors d'une inscription au endpoint `/register`.

#### 2.3 AuthResponse

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/dto/AuthResponse.java`

```java
public record AuthResponse(
        String token) {}
```

**Explication** :
- Contient le token JWT généré après une authentification réussie
- `token`: Le JWT signé que le client doit stocker et envoyer dans les requêtes ultérieures

**Utilisation** : Renvoyé au client après une connexion ou inscription réussie. Le client doit inclure ce token dans l'en-tête `Authorization: Bearer <token>` pour accéder aux endpoints protégés.

---

### 3. Repository: UserRepository

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/repository/UserRepository.java`

```java
@Repository
public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email );
    boolean existsByEmail(String email);
}
```

**Explication** :
- `@Repository`: Indique que c'est un composant de la couche d'accès aux données
- `extends JpaRepository<AppUser, Long>`: Hérite des méthodes CRUD de base (save, findAll, findById, delete, etc.)
- `AppUser`: Type de l'entité
- `Long`: Type de l'identifiant

**Méthodes Personnalisées** :
- `Optional<AppUser> findByEmail(String email)`: Recherche un utilisateur par son email. Retourne un `Optional` pour gérer le cas où l'utilisateur n'existe pas.
- `boolean existsByEmail(String email)`: Vérifie si un utilisateur avec cet email existe déjà. Utile pour éviter les doublons lors de l'inscription.

**Avantages de Spring Data JPA** :
- Pas besoin d'implémentation manuelle, Spring génère automatiquement le SQL
- Méthodes de requête dérivées du nom de la méthode
- Support pour pagination, tri, et requêtes complexes

---

### 4. Services

#### 4.1 JwtService

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/service/JwtService.java`

Service responsable de la génération, la validation et l'extraction des informations des tokens JWT.

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
- `@Value("jwt.secret")`: Injection de la clé secrète depuis application.properties (note: il y a un bug ici, devrait être `${jwt.secret}`)
- `@Value("${jwt.expiration}")`: Injection du temps d'expiration du token en millisecondes

**Méthodes** :

1. **getSecretKey()**:
   - Décode la clé secrète encodée en Base64
   - Crée une clé HMAC-SHA pour signer les tokens
   - Utilise l'algorithme de signature symétrique

2. **generateToken(UserDetails userDetails)**:
   - Génère un nouveau token JWT pour l'utilisateur
   - **subject**: L'email de l'utilisateur (utilisé comme identifiant)
   - **issuedAt**: Date de génération du token
   - **expiration**: Date d'expiration (date actuelle + jwtExpiration)
   - **signWith**: Signe le token avec la clé secrète
   - **compact**: Construit et sérialise le token en String

3. **extractUsername(String token)**:
   - Extrait le subject (email) du token
   - Utile pour identifier l'utilisateur à partir du token

4. **extractAllClaimsFromToken(String token)**:
   - Parse et vérifie la signature du token
   - Extrait toutes les claims (informations) contenues dans le token
   - Retourne l'objet Claims contenant subject, issuedAt, expiration, etc.

5. **isTokenExpired(String token, UserDetails userDetails)**:
   - Vérifie si le token appartient à l'utilisateur donné
   - Compare le username extrait du token avec celui de UserDetails
   - **Note**: Le nom de la méthode est trompeur, elle devrait s'appeler `isTokenValidForUser`

6. **isTokenExpired(String token)**:
   - Vérifie si le token est expiré
   - Compare la date d'expiration avec la date actuelle
   - **Note**: Le nom de la méthode est trompeur, elle devrait s'appeler `isTokenExpired` (retourne true si expiré)

**Structure d'un Token JWT Généré** :
```
Header.Payload.Signature

Header: {"alg":"HS256","typ":"JWT"}
Payload: {"sub":"email@example.com","iat":1234567890,"exp":1234567890+3600000}
Signature: HMACSHA256(base64UrlEncode(header) + "." + base64UrlEncode(payload), secret)
```

**Problèmes Identifiés** :
- Ligne 20: `@Value("jwt.secret")` devrait être `@Value("${jwt.secret}")` pour lire la propriété correctement
- Noms de méthodes confus (`isTokenExpired` avec deux signatures différentes)
- Pas de gestion des exceptions lors du parsing du token

---

#### 4.2 CustomerUserDetailsService

**Fichier**: `src/main/java/hiou/hicham/jwtspringapplication/service/CustomerUserDetailsService.java`

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
- Interface de Spring Security pour charger les détails de l'utilisateur
- Méthode `loadUserByUsername` appelée automatiquement par Spring Security lors de l'authentification
- Doit retourner un objet `UserDetails` contenant les informations nécessaires à l'authentification

**Fonctionnement** :

1. **Injection de Dépendances** :
   - `UserRepository` injecté via le constructeur
   - Pattern recommandé pour les tests et l'immuabilité

2. **loadUserByUsername(String email)** :
   - Recherche l'utilisateur dans la base de données par email
   - Si non trouvé, lance `UsernameNotFoundException`
   - Si trouvé, construit un objet `UserDetails` de Spring Security

3. **Construction de UserDetails** :
   - `username`: L'email de l'utilisateur (utilisé comme identifiant)
   - `password`: Le mot de passe hashé de l'utilisateur
   - `roles`: Les rôles de l'utilisateur pour les autorisations
   - Le builder `User.builder()` crée une implémentation par défaut de UserDetails

**Intégration avec Spring Security** :
- Ce service sera utilisé par `DaoAuthenticationProvider`
- Spring Security l'utilise pour :
  - Vérifier les identifiants lors de la connexion
  - Charger les autorisations (rôles) de l'utilisateur
  - Construire le contexte de sécurité (SecurityContext)

**Note** : Il manque l'annotation `@Service` sur cette classe, elle devrait être ajoutée pour que Spring puisse la détecter et l'injecter.

## Flux d'Authentification et d'Inscription

### 1. Processus d'Inscription (Register)

**Endpoint Prévu**: `POST /api/auth/register`

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
       │                       │  Valider DTO          │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Vérifier si email    │                       │
       │                       │  existe déjà          │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │                       │
       │                       │  existsByEmail()       │                       │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  Si existe: Erreur    │                       │
       │                       │  Sinon: Continuer      │                       │
       │                       │                       │                       │
       │                       │  Hasher le password    │                       │
       │                       │  (BCrypt)             │                       │
       │                       │                       │                       │
       │                       │  Créer AppUser         │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │    INSERT INTO users  │
       │                       │                       │                       │
       │                       │  Générer Token JWT     │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Retourner AuthResponse│                       │
       │                       │  {token}              │                       │
       │<──────────────────────│                       │                       │
       │                       │                       │                       │
       │  Stocker token         │                       │                       │
       │  (localStorage/cookie) │                       │                       │
```

#### Implémentation Prévue (AuthController)

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email déjà utilisé");
        }
        
        // 2. Hasher le mot de passe
        String hashedPassword = passwordEncoder.encode(request.password());
        
        // 3. Créer l'utilisateur
        AppUser user = new AppUser();
        user.setEmail(request.email());
        user.setPassword(hashedPassword);
        user.setRole("USER"); // Rôle par défaut
        
        userRepository.save(user);
        
        // 4. Générer le token JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        
        // 5. Retourner la réponse avec le token
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
```

#### Validation à Implémenter

- **Validation de l'email** : Format email valide
- **Validation du password** : Longueur minimale, complexité
- **Vérification d'unicité** : Email non déjà utilisé
- **Hashage** : Utilisation de BCryptPasswordEncoder

---

### 2. Processus de Connexion (Login)

**Endpoint Prévu**: `POST /api/auth/login`

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
       │                       │  Authentifier via     │                       │
       │                       │  AuthenticationManager│                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  loadUserByUsername() │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │  SELECT * FROM users  │
       │                       │                       │  WHERE email = ?      │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  Vérifier password     │                       │
       │                       │  (BCrypt check)        │                       │
       │                       │                       │                       │
       │                       │  Si OK: Générer Token │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  generateToken()      │                       │
       │                       │<──────────────────────│                       │
       │                       │                       │                       │
       │                       │  Retourner AuthResponse│                       │
       │  {token}              │                       │                       │
       │<──────────────────────│                       │                       │
       │                       │                       │                       │
       │  Stocker token         │                       │                       │
```

#### Implémentation Prévue (AuthController)

```java
@PostMapping("/login")
public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
    // 1. Authentifier l'utilisateur via AuthenticationManager
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            request.email(),
            request.password()
        )
    );
    
    // 2. Si authentification réussie, charger l'utilisateur
    UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
    
    // 3. Générer le token JWT
    String token = jwtService.generateToken(userDetails);
    
    // 4. Retourner la réponse avec le token
    return ResponseEntity.ok(new AuthResponse(token));
}
```

#### Mécanisme d'Authentification Spring Security

Spring Security utilise le processus suivant :
1. **AuthenticationManager** : Coordonne le processus d'authentification
2. **DaoAuthenticationProvider** : Utilise UserDetailsService pour charger l'utilisateur
3. **PasswordEncoder** : Compare le mot de passe fourni avec le hash stocké
4. **SecurityContext** : Stocke l'Authentication si succès

---

### 3. Accès aux Endpoints Protégés

**Description** : Une fois authentifié, le client peut accéder aux endpoints protégés en incluant le token JWT dans l'en-tête Authorization.

#### Étapes de l'Accès Protégé

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
       │                       │  Extraire token du     │                       │
       │                       │  header Authorization │                       │
       │                       │                       │                       │
       │                       │  Valider token         │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  extractUsername()     │                       │
       │                       │<──────────────────────│                       │
       │                       │                       │                       │
       │                       │  Charger utilisateur   │                       │
       │                       │───────────────────────┼──────────────────────>│
       │                       │                       │                       │
       │                       │  loadUserByUsername()  │                       │
       │                       │<──────────────────────┼───────────────────────│
       │                       │                       │                       │
       │                       │  Créer Authentication  │                       │
       │                       │  et SecurityContext    │                       │
       │                       │                       │                       │
       │                       │  Laisser passer la     │                       │
       │                       │  requête au Controller │                       │
       │                       │──────────────────────>│                       │
       │                       │                       │                       │
       │                       │  Traiter la requête    │                       │
       │  Response             │                       │                       │
       │<──────────────────────│                       │                       │
```

#### Implémentation Prévue (JwtAuthenticationFilter)

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        
        // 1. Extraire le token du header Authorization
        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtService.extractUsername(token);
        }
        
        // 2. Si token présent et utilisateur non authentifié
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 3. Charger les détails de l'utilisateur
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            // 4. Valider le token
            if (jwtService.isTokenExpired(token, userDetails) && !jwtService.isTokenExpired(token)) {
                // 5. Créer l'objet Authentication
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                
                // 6. Définir le contexte de sécurité
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        // 7. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
}
```

#### Utilisation du Token par le Client

```javascript
// Exemple en JavaScript (Frontend)
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

### 4. Configuration de Sécurité Spring

**Description** : La configuration de Spring Security définit les règles de sécurité, les filtres, et les endpoints publics/protégés.

#### Implémentation Prévue (SecurityConfiguration)

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
            .csrf(csrf -> csrf.disable()) // Désactiver CSRF pour API REST
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll() // Endpoints publics
                .requestMatchers("/h2-console/**").permitAll() // Console H2 publique
                .anyRequest().authenticated() // Tous les autres endpoints protégés
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Pas de session HTTP
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class); // Ajouter filtre JWT
        
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
2. **permitAll()** : Permet l'accès sans authentification aux endpoints spécifiés
3. **authenticated()** : Nécessite une authentification pour tous les autres endpoints
4. **STATELESS** : Indique que l'application n'utilise pas de sessions HTTP
5. **addFilterBefore()** : Ajoute le filtre JWT avant le filtre d'authentification par défaut
6. **BCryptPasswordEncoder** : Encodeur de mot de passe sécurisé

---

## Configuration

### application.properties

```properties
# Configuration de la base de données H2
spring.datasource.url=jdbc:h2:mem:securitydbdemo
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# Configuration JPA
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update

# Console H2
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Configuration JWT
jwt.secret=TXlKV1RTZWNyZXRIaWNoYW1PdWFvdWNoZWhkaGFoaGNIR0d3VVVWQkhZR1NIWVhVVUNCRw==
jwt.expiration=3600000
```

#### Explication des Propriétés

**Base de Données H2** :
- `spring.datasource.url`: URL de connexion à la base de données H2 en mémoire
- `spring.datasource.driverClassName`: Driver JDBC pour H2
- `spring.datasource.username`: Nom d'utilisateur par défaut de H2
- `spring.datasource.password`: Mot de passe vide par défaut

**Configuration JPA** :
- `spring.jpa.show-sql=true`: Affiche les requêtes SQL dans la console (utile pour le débogage)
- `spring.jpa.hibernate.ddl-auto=update`: Met automatiquement à jour le schéma de la base de données

**Console H2** :
- `spring.h2.console.enabled=true`: Active la console web H2
- `spring.h2.console.path=/h2-console`: Chemin d'accès à la console

**Configuration JWT** :
- `jwt.secret`: Clé secrète encodée en Base64 pour signer les tokens (DOIT être gardée secrète en production)
- `jwt.expiration=3600000`: Durée de validité du token en millisecondes (1 heure = 3600000 ms)

**Note de Sécurité** : En production, la clé secrète JWT ne doit jamais être stockée dans le code ou dans un fichier de configuration versionné. Elle devrait être injectée via des variables d'environnement ou un service de gestion de secrets.

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

**Explication** :
- `jjwt-api`: API pour la création et le parsing des tokens JWT
- `jjwt-impl`: Implémentation de l'API (nécessaire à l'exécution)
- `jjwt-jackson`: Intégration avec Jackson pour la sérialisation JSON

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

**Explication** :
- `spring-boot-starter-data-jpa`: Fournit JPA et Hibernate pour la persistance
- `spring-boot-starter-security`: Intégration de Spring Security
- `spring-boot-starter-webmvc`: Fournit Spring MVC pour les API REST

#### Base de Données

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>runtime</scope>
</dependency>
```

**Explication** :
- Base de données H2 en mémoire pour le développement et les tests
- Scope `runtime` car elle n'est nécessaire qu'à l'exécution

#### Utilitaires

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

**Explication** :
- Lombok réduit le code boilerplate (getters, setters, constructeurs)
- Annotation processor configuré dans le plugin maven-compiler-plugin

---

## État Actuel du Projet

### Composants Implémentés ✅

1. **Entity AppUser** : Entité utilisateur avec mappage JPA
2. **DTOs** : LoginRequest, RegisterRequest, AuthResponse
3. **UserRepository** : Repository JPA avec méthodes personnalisées
4. **JwtService** : Service complet pour la gestion des tokens JWT
5. **CustomerUserDetailsService** : Service de chargement des utilisateurs
6. **Configuration application.properties** : Configuration de la base de données et JWT

### Composants Manquants ❌

1. **AuthController** : Controller REST pour les endpoints /register et /login
2. **UserController** : Controller pour les endpoints protégés
3. **SecurityConfiguration** : Configuration de Spring Security
4. **JwtAuthenticationFilter** : Filtre pour intercepter et valider les tokens JWT
5. **PasswordEncoder** : Bean pour le hashage des mots de passe (BCrypt)
6. **Validation des DTOs** : Annotations de validation (@Valid, @Email, @Size, etc.)
7. **Gestion des exceptions** : Handler global pour les exceptions
8. **Tests** : Tests unitaires et d'intégration

### Problèmes Identifiés dans le Code Existant ⚠️

1. **JwtService.java - Ligne 20** :
   ```java
   @Value("jwt.secret")  // ❌ Incorrect
   ```
   Devrait être :
   ```java
   @Value("${jwt.secret}")  // ✅ Correct
   ```

2. **CustomerUserDetailsService.java** :
   - Manque l'annotation `@Service`
   - Spring ne pourra pas détecter et injecter ce bean automatiquement

3. **Noms de méthodes confus dans JwtService** :
   - `isTokenExpired(String token, UserDetails)` : Vérifie si le token appartient à l'utilisateur (pas l'expiration)
   - `isTokenExpired(String token)` : Vérifie l'expiration du token
   - Devraient être renommées pour plus de clarté

4. **Pas de hashage des mots de passe** :
   - Les mots de passe sont stockés en clair
   - Risque de sécurité critique

5. **Pas de gestion des exceptions** :
   - Pas de try-catch lors du parsing des tokens
   - Risque d'exceptions non gérées en production

---

## Sécurité : Analyse et Recommandations

### JWT (JSON Web Tokens)

#### Avantages
- **Stateless** : Pas besoin de stocker les sessions côté serveur
- **Scalable** : Facile à distribuer sur plusieurs serveurs
- **Standard** : Format standard (RFC 7519)
- **Flexible** : Peut contenir des claims personnalisées

#### Inconvénients et Risques
- **Non révocable** : Une fois émis, un token est valide jusqu'à son expiration
- **Vulnérable si compromis** : Si la clé secrète est compromise, tous les tokens peuvent être forgés
- **Taille** : Plus grand qu'un simple session ID
- **Stockage côté client** : Risque de XSS si stocké dans localStorage

#### Bonnes Pratiques
1. **Toujours utiliser HTTPS** : Empêcher l'interception des tokens
2. **Expiration courte** : 15-30 minutes pour les tokens d'accès
3. **Refresh Tokens** : Implémenter des refresh tokens avec expiration plus longue
4. **Stockage sécurisé** : Préférer httpOnly cookies au localStorage
5. **Validation stricte** : Vérifier la signature, l'expiration et l'émetteur
6. **Claims minimales** : Ne stocker que les informations nécessaires dans le token

### Mot de Passe

#### Problème Actuel
Les mots de passe sont stockés en clair dans la base de données, ce qui représente un risque critique de sécurité.

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
- **Sûr** : Utilise un sel automatique et est résistant aux attaques par rainbow table
- **Standard de l'industrie** : largement adopté et testé

#### Exemple d'Utilisation

```java
// Hashage lors de l'inscription
String hashedPassword = passwordEncoder.encode(rawPassword);

// Vérification lors de la connexion
boolean matches = passwordEncoder.matches(rawPassword, hashedPassword);
```

### Base de Données

#### H2 en Mémoire
- **Avantages** : Rapide, facile à configurer, pas d'installation requise
- **Inconvénients** : Données perdues au redémarrage, pas adapté à la production

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
2. **Rate Limiting** : Limiter les tentatives de connexion pour prévenir les attaques par force brute
3. **Logging Sécurisé** : Ne jamais logger les mots de passe ou des informations sensibles
4. **CORS** : Configurer correctement pour les applications frontend
5. **Headers de Sécurité** : Utiliser des headers comme X-Content-Type-Options, X-Frame-Options

---

## Tests et Validation

### Tests Unitaires Préconisés

1. **JwtServiceTest** :
   - Tester la génération de token
   - Tester l'extraction du username
   - Tester la validation de l'expiration
   - Tester avec des tokens invalides

2. **CustomerUserDetailsServiceTest** :
   - Tester le chargement d'un utilisateur existant
   - Tester le comportement avec un utilisateur inexistant
   - Tester la construction de UserDetails

3. **UserRepositoryTest** :
   - Tester la méthode findByEmail
   - Tester la méthode existsByEmail
   - Tester la création et la suppression d'utilisateurs

### Tests d'Intégration Préconisés

1. **AuthControllerIntegrationTest** :
   - Tester le flux d'inscription complet
   - Tester le flux de connexion complet
   - Tester les erreurs (email déjà utilisé, mauvais mot de passe)

2. **SecurityIntegrationTest** :
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
# Cloner le repository
git clone <repository-url>
cd JwtSpringApplication

# Lancer l'application
./mvnw spring-boot:run

# Accéder à l'application
# API: http://localhost:8080
# Console H2: http://localhost:8080/h2-console
```

### Configuration pour la Production

1. **Variables d'Environnement** :
```bash
export JWT_SECRET=votre_clé_secrète_très_longue_et_aleatoire
export JWT_EXPIRATION=3600000
export DB_URL=jdbc:postgresql://votre-db:5432/securitydb
export DB_USERNAME=votre_username
export DB_PASSWORD=votre_password
```

2. **application-production.properties** :
```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}
```

3. **Build pour la Production** :
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

---

## Améliorations Futures

### Court Terme (Priorité Haute)

- [ ] **Corriger les bugs identifiés** :
  - Corriger `@Value("jwt.secret")` en `@Value("${jwt.secret}")`
  - Ajouter `@Service` sur CustomerUserDetailsService
  - Renommer les méthodes confuses dans JwtService

- [ ] **Implémenter AuthController** :
  - Endpoint POST /api/auth/register
  - Endpoint POST /api/auth/login
  - Validation des DTOs

- [ ] **Implémenter SecurityConfiguration** :
  - Configuration de SecurityFilterChain
  - Bean PasswordEncoder (BCrypt)
  - Bean AuthenticationManager

- [ ] **Implémenter JwtAuthenticationFilter** :
  - Extraction du token du header Authorization
  - Validation du token
  - Création du SecurityContext

- [ ] **Ajouter le hashage des mots de passe** :
  - Utiliser BCryptPasswordEncoder
  - Hasher lors de l'inscription
  - Vérifier lors de la connexion

### Moyen Terme (Priorité Moyenne)

- [ ] **Implémenter UserController** :
  - Endpoint GET /api/user/profile (protégé)
  - Endpoint PUT /api/user/profile (protégé)
  - Endpoint DELETE /api/user/account (protégé)

- [ ] **Gestion des Rôles** :
  - Implémenter différents rôles (USER, ADMIN)
  - Endpoints spécifiques par rôle
  - Autorisations basées sur les rôles

- [ ] **Refresh Token** :
  - Implémenter des refresh tokens
  - Endpoint POST /api/auth/refresh
  - Rotation des tokens

- [ ] **Validation des DTOs** :
  - Ajouter @Valid sur les paramètres
  - Annotations @Email, @Size, @NotBlank
  - Messages d'erreur personnalisés

- [ ] **Gestion des Exceptions** :
  - @ControllerAdvice global
  - Exceptions personnalisées
  - Réponses d'erreur structurées

### Long Terme (Priorité Basse)

- [ ] **Migration vers une base de données persistante** :
  - PostgreSQL ou MySQL
  - Configuration de production
  - Migrations de base de données (Flyway/Liquibase)

- [ ] **Tests Complets** :
  - Tests unitaires pour tous les services
  - Tests d'intégration pour les controllers
  - Tests de sécurité

- [ ] **Rate Limiting** :
  - Limiter les tentatives de connexion
  - Limiter les requêtes par IP
  - Utiliser Spring Boot Starter for Resilience4j

- [ ] **Audit Logging** :
  - Logger les connexions réussies/échouées
  - Logger les actions sensibles
  - Conformité RGPD

- [ ] **OAuth2 / OpenID Connect** :
  - Support de l'authentification tiers (Google, GitHub)
  - Intégration avec des providers d'identité

- [ ] **2FA (Two-Factor Authentication)** :
  - Implémenter TOTP (Time-based One-Time Password)
  - SMS ou Email pour le code de vérification

---

## Conclusion

JwtSpringApplication est un projet éducatif qui démontre l'implémentation d'un système d'authentification JWT avec Spring Boot. Bien que les fondations soient solides, le projet nécessite encore plusieurs composants pour être fonctionnel et sécurisé en production.

### Points Forts

- ✅ Architecture claire et bien structurée
- ✅ Utilisation des standards (JWT, Spring Security, JPA)
- ✅ Code lisible et maintenable
- ✅ Services JWT bien implémentés

### Points à Améliorer

- ❌ Controllers REST manquants
- ❌ Configuration Spring Security incomplète
- ❌ Pas de hashage des mots de passe
- ❌ Bugs mineurs dans le code existant
- ❌ Pas de tests

### Recommandations

1. **Prioriser la sécurité** : Implémenter BCrypt pour les mots de passe avant toute autre fonctionnalité
2. **Compléter l'authentification** : Implémenter AuthController et SecurityConfiguration
3. **Ajouter des tests** : Écrire des tests unitaires et d'intégration
4. **Corriger les bugs** : Résoudre les problèmes identifiés dans le code existant
5. **Documenter l'API** : Utiliser Swagger/OpenAPI pour documenter les endpoints

Ce projet constitue une excellente base pour comprendre l'authentification JWT avec Spring Security et peut être étendu pour devenir une application de production complète et sécurisée.

---

## Auteur

Projet réalisé dans le cadre du cours de sécurité des applications (SDIA - 2 ans).

## Licence

Ce projet est à usage éducatif.
