# 🚗 BidAndDrive

Plateforme d'enchères de voitures, réalisée dans le cadre du kata Carrefour.
Un vendeur met sa voiture aux enchères. Des acheteurs enchérissent, plusieurs à la fois sur la même voiture, et voient les nouvelles offres en direct.

- **Backend** : Java 25, Spring Boot 4 (Spring MVC, Spring HATEOAS, Spring Data JPA), Flyway
- **Frontend** : Vue 3, TypeScript, Vite, Pinia, Vue Router, Tailwind CSS
- **Persistance** : PostgreSQL 18
- **Déploiement** : Railway, via GitHub Actions
- **Harness agentique** : Claude Code (`CLAUDE.md`, skills, agents et hooks dans `.claude/`)

## Sommaire
- [Fonctionnalités](#fonctionnalités)
- [Démarrer le projet](#démarrer-le-projet)
- [Où trouver quoi](#où-trouver-quoi)
- [API REST](#api-rest)
- [Choix techniques](#choix-techniques)
- [Méthode de travail](#méthode-de-travail)
- [Limites connues et suites possibles](#limites-connues-et-suites-possibles)

## Fonctionnalités

Chaque fonctionnalité part d'une spécification numérotée dans [docs/specs/](docs/specs/README.md), qui fait foi.

| # | Fonctionnalité | État |
|---|---|---|
| 003 | Identité : utilisateurs prédéfinis et sélecteur d'utilisateur | ✅ |
| 004 | Mettre une voiture aux enchères (story MVP vendeur) | ✅ |
| 005 | Lister les enchères ouvertes, paginées | ✅ |
| 006 | Détail d'une enchère : voiture, prix, historique des offres | ✅ |
| 007 | Enchérir, avec gestion des offres concurrentes (story MVP acheteur) | ✅ |
| 008 | Mes enchères : où j'en suis sur chaque voiture | ✅ |
| 009 | Événements d'enchère en direct (Server-Sent Events) | ✅ |
| 010 | Page d'enchère en direct, notification quand on est surenchéri | ✅ |
| 011 | Résultat de l'enchère : vendue ou sans offre, gagné ou perdu | ✅ |
| 012 | Anti-sniping (prolongation en fin d'enchère) | 📝 spécifiée, non implémentée |
| 013 | Liste de suivi (watchlist) | 📝 spécifiée, non implémentée |

## Démarrer le projet

### Prérequis
- Java 25
- Node.js 24 et npm
- Docker (pour PostgreSQL en local et pour les tests d'intégration via Testcontainers)

### Lancement en local

```bash
# 1. Base de données
docker compose up -d postgres

# 2. Backend sur http://localhost:8080
cd backend
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run

# 3. Frontend sur http://localhost:5173 (autre terminal)
cd frontend
npm install
npm run dev
```

Le serveur Vite redirige `/api` vers le backend : il suffit d'ouvrir http://localhost:5173.

Flyway crée le schéma et les quatre utilisateurs (Alice, Bob, Chloe, Dan) au démarrage.
Le profil `local` fournit les identifiants de la base Docker. Sans lui, le backend exige `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` et refuse de démarrer s'ils manquent.

Variante tout-Docker pour le backend : `docker compose up -d` lance PostgreSQL et le backend ensemble.

### Données de démonstration (optionnel)

[scripts/seed-demo.sql](scripts/seed-demo.sql) remplit la place de marché : enchères terminées (vendues ou non), enchères qui se terminent dans l'heure, enchères ouvertes avec et sans offres.

```bash
docker compose exec -T postgres psql -U bidanddrive -d bidanddrive < scripts/seed-demo.sql
```

### Essayer les enchères concurrentes

L'utilisateur choisi est gardé par onglet. Ouvrez la même enchère dans deux onglets, choisissez Alice dans l'un et Bob dans l'autre avec le sélecteur en haut de page, puis enchérissez : chaque onglet voit l'offre de l'autre arriver en direct.

### Tests et qualité

```bash
# Backend : tests unitaires, tests de slice et tests d'intégration sur un vrai PostgreSQL (Testcontainers)
cd backend && ./mvnw verify

# Frontend : vérification de types, build, tests unitaires, lint
cd frontend && npm run build && npm run test:unit && npm run lint
```

La CI ([.github/workflows/ci.yml](.github/workflows/ci.yml)) lance ces deux suites sur chaque pull request.

### Déploiement

- [deploy/railway.sh](deploy/railway.sh) crée une seule fois le projet Railway : PostgreSQL, backend privé, frontend public.
- [.github/workflows/deploy.yml](.github/workflows/deploy.yml) déploie `main` à chaque push.
- Le nginx du frontend sert l'application et relaie `/api/` vers le backend par le réseau privé de Railway. Le navigateur ne voit qu'une origine, donc pas de CORS. Détails dans l'[ADR 0012](docs/adr/0012-railway-deployment.md).

## Où trouver quoi

```
.
├── backend/                 API Spring Boot
├── frontend/                Application Vue 3
├── docs/
│   ├── specs/               Une spec numérotée par fonctionnalité (+ feuille de route)
│   └── adr/                 Décisions d'architecture (ADR)
├── deploy/                  Bootstrap Railway
├── scripts/                 Données de démonstration
├── .claude/                 Harness Claude Code : skills, agents, hooks, permissions
├── .github/workflows/       CI et déploiement
├── docker-compose.yml       PostgreSQL (et backend) en local
└── CLAUDE.md                Règles du projet pour l'agent (avec backend/ et frontend/CLAUDE.md)
```

### Backend : architecture hexagonale par domaine

Package racine `com.carrefour.kata.bidanddrive`, découpé par domaine métier puis par couche :

```
auction/
├── domain/                  Règles métier pures, sans Spring ni JPA
│                            Auction, Bid, Money, AuctionWindow, AuctionPhase, AuctionOutcome…
├── application/             Cas d'usage et ports
│                            CreateAuction, PlaceBid, ListOpenAuctions, GetAuctionDetail, ListMyBids…
│                            AuctionRepository, BidRepository, AuctionEventPublisher (interfaces)
└── adapter/
    ├── in/web/              Contrôleurs REST minces, DTO en records, gestion des erreurs
    ├── out/persistence/     Entités JPA, repositories, mapping vers le domaine
    ├── out/events/          Diffusion SSE : broker en mémoire, relais après commit
    └── config/              Câblage Spring et propriétés
identity/                    Même découpage : utilisateurs et résolution de X-User-Id
shared/                      Transverse : Clock, configuration Jackson
```

Les migrations Flyway sont dans [backend/src/main/resources/db/migration](backend/src/main/resources/db/migration).

| Fonctionnalité | Cas d'usage | Contrôleur |
|---|---|---|
| Créer une enchère | `CreateAuction` | `AuctionController` |
| Lister / détail | `ListOpenAuctions`, `GetAuctionDetail` | `AuctionController` |
| Enchérir | `PlaceBid`, `GetBid` | `BidController` |
| Mes enchères | `ListMyBids` | `MyBidsController` |
| Direct | `AuctionEventPublisher` | `AuctionEventsController` |
| Utilisateurs | `ListUsers` | `UserController` |

### Frontend : dossiers par fonctionnalité

```
src/
├── features/
│   ├── auction/             Liste, détail, création, flux temps réel, bandeau de résultat
│   │   ├── views/           AuctionsView, AuctionDetailView, CreateAuctionView
│   │   ├── components/      AuctionCard, AuctionForm, BidHistory, PriceSummary…
│   │   └── composables/     useAuctionDetail, useAuctionStream, useCreateAuctionForm
│   ├── bidding/             Panneau d'enchère, « Mes enchères »
│   └── identity/            Store Pinia de l'utilisateur courant
├── components/              UI partagée : AppHeader, UserSwitcher, AppToast
├── composables/             Composables partagés (useNow)
├── api/                     Client HTTP typé ; les composants n'appellent jamais fetch
└── router/                  Routes
```

| Route | Écran |
|---|---|
| `/` | Enchères ouvertes |
| `/auctions/new` | Mettre une voiture aux enchères |
| `/auctions/:id` | Enchère en direct |
| `/me/bids` | Mes enchères |

## API REST

Toutes les routes sont sous `/api`. Les réponses suivent HAL (`_links`) et les collections sont paginées (`page` à partir de 0, `size` 20 par défaut, 100 au maximum).

| Méthode | Route | Rôle | Identité |
|---|---|---|---|
| `GET` | `/api/users` | Utilisateurs du sélecteur | — |
| `POST` | `/api/auctions` | Créer une enchère | requise |
| `GET` | `/api/auctions` | Enchères ouvertes | optionnelle |
| `GET` | `/api/auctions/{id}` | Détail, historique, résultat | optionnelle |
| `GET` | `/api/auctions/{id}/events` | Flux SSE des offres | — |
| `POST` | `/api/auctions/{id}/bids` | Enchérir | requise |
| `GET` | `/api/auctions/{id}/bids/{bidId}` | Une offre (visible par son auteur seul) | requise |
| `GET` | `/api/me/bids` | Mes enchères | requise |

L'identité passe par l'en-tête `X-User-Id`. Les erreurs suivent RFC 9457 (`ProblemDetail`) : 400 entrée invalide, 401 identité absente ou inconnue, 403 vendeur qui enchérit sur sa voiture, 404 ressource inconnue, 409 conflit d'état (enchère close, déjà meilleur enchérisseur, offre concurrente), 422 montant sous le minimum.

Le contrat détaillé de chaque route (corps, liens, cas d'erreur) est dans sa spec.

## Choix techniques

Le kata laisse beaucoup de points ouverts. Chaque choix est consigné dans un ADR de [docs/adr/](docs/adr/). Les principaux :

### Architecture
- **Hexagonale, domaine pur.** Le domaine ne dépend ni de Spring ni de JPA, ce qui le rend testable sans contexte Spring. Les entités JPA sont séparées des objets métier et mappées dans l'adaptateur de persistance.
- **Argent en `BigDecimal`, échelle 2, transmis en chaîne JSON** (`"12600.00"`) pour éviter les arrondis des nombres flottants en JavaScript. Le frontend ne fait pas de calcul sur l'argent ([ADR 0004](docs/adr/0004-money-as-json-string.md)).
- **Temps en `Instant` avec une `Clock` injectée**, jamais `Instant.now()` dans le domaine : les règles de date se testent avec une horloge fixe.
- **Flyway possède le schéma**, Hibernate ne fait que le valider.

### Enchères concurrentes
- **Verrouillage optimiste** (`@Version` sur l'enchère). Deux offres basées sur le même prix ne peuvent pas passer toutes les deux : la perdante reçoit un 409 et l'acheteur voit le nouveau prix avant de réessayer. Pas de nouvelle tentative automatique côté serveur. Un test d'intégration lance des offres en parallèle pour le prouver ([ADR 0003](docs/adr/0003-optimistic-locking-for-bids.md)).

### Règles métier choisies
- **Tout utilisateur peut vendre et acheter.** Le rôle dépend de l'enchère, pas de l'utilisateur ([ADR 0009](docs/adr/0009-any-user-may-sell.md)).
- **Le vendeur ne peut pas enchérir sur sa voiture**, **on ne peut pas surenchérir sur soi-même**, **la première offre peut être égale au prix de départ**, les suivantes ajoutent au moins l'incrément minimum ([ADR 0006](docs/adr/0006-bidding-rules.md)).
- **Durée d'une enchère : de 1 heure à 30 jours**, configurable ([ADR 0008](docs/adr/0008-auction-end-window.md)).
- **Ordre des contrôles et codes HTTP fixés** pour qu'une offre refusée donne toujours la même réponse ([ADR 0005](docs/adr/0005-bid-error-mapping.md)).
- **Le résultat d'une enchère est calculé à la lecture** : elle est close dès que `endsAt` est passé, et le gagnant se déduit des offres. Pas de tâche planifiée qui fermerait les enchères et entrerait en course avec les dernières offres ([ADR 0016](docs/adr/0016-auction-result-on-read.md)).

### Identité
- **Pas d'authentification réelle** : quatre utilisateurs prédéfinis et l'en-tête `X-User-Id`, choisi via un sélecteur. L'utilisateur est stocké en `sessionStorage`, donc par onglet, pour simuler plusieurs acheteurs dans un seul navigateur. Le domaine ne voit qu'un `UserId` : une vraie authentification pourra remplacer le résolveur sans toucher au métier ([ADR 0001](docs/adr/0001-identity-user-switcher.md)).
- Les lectures publiques acceptent une identité absente ou invalide comme anonyme ([ADR 0014](docs/adr/0014-optional-identity-on-public-reads.md)). Une offre n'est lisible que par son auteur ([ADR 0013](docs/adr/0013-bid-resource-visibility.md)).

### Temps réel
- **Server-Sent Events plutôt que WebSocket.** Le flux ne va que du serveur vers le navigateur. `SseEmitter` fonctionne avec Spring MVC sans dépendance supplémentaire, et `EventSource` se reconnecte seul ([ADR 0002](docs/adr/0002-sse-over-websocket.md)).
- Les événements sont publiés **après le commit** de l'offre, par un broker en mémoire derrière un port applicatif. Un heartbeat garde les connexions ouvertes. Une erreur d'envoi vers un client ne peut pas transformer une offre validée en erreur ([ADR 0015](docs/adr/0015-sse-event-delivery.md)).

### API
- **HATEOAS (HAL)** : les liens disent au client ce qu'il peut faire. Par exemple le lien `place-bid` n'apparaît que si l'enchère accepte des offres. Exception assumée : `/api/users` renvoie un simple tableau ([ADR 0007](docs/adr/0007-plain-user-list.md)).
- **Erreurs de validation groupées** dans un `ProblemDetail` avec une liste `errors` de `{field, message}`, pour afficher toutes les erreurs d'un formulaire en une fois ([ADR 0011](docs/adr/0011-validation-error-shape.md)).

### Tests
- Backend : JUnit 5 et AssertJ. Le domaine est testé unitairement, les contrôleurs par des tests de slice, la persistance et les scénarios complets contre un **vrai PostgreSQL via Testcontainers**, jamais une base en mémoire.
- Frontend : Vitest et Testing Library. On teste le comportement visible, pas les détails d'implémentation.
- Chaque critère d'acceptation d'une spec est couvert par au moins un test.

## Méthode de travail

Le projet a été construit avec **Claude Code** comme agent de développement. Le harness fait partie du livrable :

- [CLAUDE.md](CLAUDE.md), [backend/CLAUDE.md](backend/CLAUDE.md), [frontend/CLAUDE.md](frontend/CLAUDE.md) : règles du projet, architecture, conventions git.
- [.claude/skills/](.claude/skills/) : procédures réutilisables. Workflow (`feature`, `plan-feature`, `validate-plan`, `implement-slice`, `test`, `commit`, `open-pr`, `review-pr`) et conventions techniques (Spring Boot, Java 25, JPA, tests, Vue 3, design).
- [.claude/agents/](.claude/agents/) : `plan-critic` relit une spec de façon critique avant tout code, `code-reviewer` relit le diff d'une PR dans un contexte vierge.
- [.claude/hooks/](.claude/hooks/) : garde-fous sur les commandes shell (pas de push forcé, pas de `--no-verify`…) et formatage automatique du frontend.

Le cycle d'une fonctionnalité :

1. **Spec** : critères d'acceptation, contrat d'API, règles métier, découpage en tranches. Validation humaine.
2. **Revue de la spec** par `plan-critic`. Go / no-go humain.
3. **Implémentation en TDD**, tranche par tranche : test rouge, code minimal, vert, refactoring.
4. **Petits commits** au format Conventional Commits, une branche et une PR par spec.
5. **Revue de la PR** par `code-reviewer`, puis merge.

Quand le kata est ambigu, un choix est fait et consigné dans un ADR.

## Limites connues et suites possibles

- **Sécurité** : l'en-tête `X-User-Id` n'est pas authentifié. Le déploiement sert à la démonstration uniquement. Suite naturelle : OAuth2 / JWT derrière le même résolveur d'identité.
- **Une seule instance** : le broker SSE est en mémoire. Plusieurs instances demanderaient un broker partagé (Redis Pub/Sub, Kafka…).
- **Pas de rejeu** des événements SSE : à la reconnexion, la page recharge l'enchère.
- **Documentation OpenAPI** et **cache** : pas encore en place.
- **Anti-sniping (012)** et **liste de suivi (013)** : spécifiées, à implémenter.
- **Fin d'enchère** : aucune action n'est déclenchée quand une enchère se termine (notification du vendeur, paiement). Cela demandera une tâche ou un événement dédié.
