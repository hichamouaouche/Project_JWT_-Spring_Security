# JwtSpringApplication

## Description

JwtSpringApplication est une application Spring Boot implémentant un système d'authentification basé sur JSON Web Tokens (JWT). Ce projet fait partie du cours de sécurité des applications et démontre l'intégration de Spring Security avec JWT pour sécuriser une API REST.

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

### Structure des Packages

```
hiou.hicham.jwtspringapplication/
├── JwtSpringApplication.java       # Classe principale de l'application
├── dto/                            # Data Transfer Objects
│   ├── AuthResponse.java           # Réponse d'authentification (token)
│   ├── LoginRequest.java           # Requête de connexion
│   └── RegisterRequest.java        # Requête d'inscription
├── entity/
│   └── AppUser.java                # Entité utilisateur
├── repository/
│   └── UserRepository.java         # Repository JPA pour les utilisateurs
└── service/
    ├── JwtService.java             # Service de gestion des tokens JWT
    └── CustomerUserDetailsService.java # Service de détails utilisateur personnalisé
```

## Composants Principaux

### 1. Entity: AppUser

L'entité `AppUser` représente un utilisateur dans le système avec les attributs suivants :
- `id`: Identifiant unique (auto-généré)
- `email`: Adresse email (unique)
- `password`: Mot de passe (hashé)
- `role`: Rôle de l'utilisateur

### 2. DTOs (Data Transfer Objects)

- **LoginRequest**: Contient l'email et le mot de passe pour la connexion
- **RegisterRequest**: Contient l'email et le mot de passe pour l'inscription
- **AuthResponse**: Contient le token JWT généré après authentification

### 3. Repository: UserRepository

Interface JPA fournissant les opérations de base de données :
- `findByEmail(String email)`: Recherche un utilisateur par email
- `existsByEmail(String email)`: Vérifie si un email existe déjà

### 4. Services

#### JwtService
Service responsable de la gestion des tokens JWT :
- `generateToken(UserDetails)`: Génère un token JWT pour un utilisateur
- `extractUsername(String token)`: Extrait le nom d'utilisateur du token
- `extractAllClaimsFromToken(String token)`: Extrait toutes les claims du token
- `isTokenExpired(String token, UserDetails)`: Vérifie si le token est valide pour l'utilisateur
- `isTokenExpired(String token)`: Vérifie si le token est expiré

#### CustomerUserDetailsService
Implémentation personnalisée de `UserDetailsService` :
- Charge les détails de l'utilisateur depuis la base de données
- Convertit l'entité `AppUser` en `UserDetails` de Spring Security

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

### Dépendances Principales

- **JWT Libraries**: jjwt-api, jjwt-impl, jjwt-jackson (0.13.0)
- **Spring Boot Starters**: webmvc, security, data-jpa, h2console
- **Base de données**: H2 (runtime)
- **Utilitaires**: Lombok (optional)

## Fonctionnalités Actuelles

### Implémentées
- ✅ Génération de tokens JWT
- ✅ Validation de tokens JWT
- ✅ Service de détails utilisateur personnalisé
- ✅ Repository JPA pour la gestion des utilisateurs
- ✅ Configuration de la base de données H2
- ✅ Structure des DTOs pour les requêtes/réponses d'authentification

### À Implémenter
- ❌ Controller REST pour l'inscription (/register)
- ❌ Controller REST pour la connexion (/login)
- ❌ Configuration de Spring Security (SecurityFilterChain)
- ❌ Filtre JWT pour l'interception des requêtes
- ❌ Hashage des mots de passe (BCrypt)
- ❌ Endpoints protégés nécessitant une authentification

## Flux d'Authentification Prévu

1. **Inscription**: L'utilisateur envoie ses identifiants via `RegisterRequest`
2. **Connexion**: L'utilisateur envoie ses identifiants via `LoginRequest`
3. **Génération Token**: Le système valide les identifiants et génère un JWT
4. **Réponse**: Le token est renvoyé dans `AuthResponse`
5. **Accès Protégé**: Le client inclut le token dans l'en-tête Authorization pour accéder aux endpoints protégés

## Sécurité

### JWT Configuration
- **Secret**: Clé secrète encodée en Base64 pour signer les tokens
- **Expiration**: 1 heure (3600000 ms)
- **Algorithme**: HMAC-SHA

### Base de Données
- Base de données H2 en mémoire (pour le développement)
- Console H2 accessible à `/h2-console`
- Schéma automatiquement mis à jour (ddl-auto=update)

## Instructions de Développement

### Prérequis
- Java 21 ou supérieur
- Maven 3.6+

### Lancer l'Application

```bash
# Utiliser Maven wrapper)
./mvnw spring-boot:run

# Ou avec Maven installé
mvn spring-boot:run
```

### Accéder à la Console H2

Une fois l'application démarrée :
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:securitydbdemo`
- Username: `sa`
- Password: (laisser vide)

### Tests

```bash
# Exécuter les tests
./mvnw test
```

## Problèmes Connus et Limitations

1. **Application Incomplète**: Les controllers REST et la configuration Spring Security ne sont pas encore implémentés
2. **Pas de Hashage**: Les mots de passe sont stockés en clair (à corriger avec BCrypt)
3. **Pas de Validation**: Aucune validation des entrées utilisateur
4. **Base de Données Volatile**: H2 en mémoire perd les données au redémarrage

## Améliorations Futures

- [ ] Implémenter AuthController avec endpoints /register et /login
- [ ] Configurer SecurityFilterChain pour Spring Security
- [ ] Créer JwtAuthenticationFilter pour intercepter les requêtes
- [ ] Ajouter le hashage des mots de passe avec BCryptPasswordEncoder
- [ ] Implémenter la validation des DTOs
- [ ] Ajouter des endpoints protégés (ex: /api/user/profile)
- [ ] Migrer vers une base de données persistante (PostgreSQL/MySQL)
- [ ] Ajouter des tests unitaires et d'intégration
- [ ] Implémenter le rafraîchissement des tokens (refresh token)
- [ ] Ajouter la gestion des rôles et autorisations

## Auteur

Projet réalisé dans le cadre du cours de sécurité des applications.

## Licence

Ce projet est à usage éducatif.
