# Ms-Gwent

Backend para una aplicación de simulación del juego **Gwent (The Witcher 3)**, basado en **microservicios con Spring Boot** y un **BFF (Backend for Frontend)** como único punto de entrada.

> **Estado del proyecto:** En desarrollo activo. auth-service, jugador-service, ingame-service y bff-service son funcionales. solicitud-service es un esqueleto vacío.

---

## Arquitectura general

- El **frontend nunca consume microservicios directamente**.
- El **BFF** expone la API pública (`/api/**`) y orquesta llamadas internas.
- La identidad del jugador (**actor**) se obtiene siempre desde autenticación.
- El `playerId` del actor **nunca viaja desde el cliente en el body**.

### Servicios

| Servicio | Puerto | Estado |
|---|---|---|
| **bff-service** | 8080 | Funcional — punto de entrada público `/api/**` |
| **jugador-service** | 8082 | Funcional — perfiles de jugador |
| **ingame-service** | 8083 | Funcional — catálogo de cartas |
| **auth-service** | 8085 | Funcional — autenticación JWT RS256 |
| **solicitud-service** | — | Esqueleto vacío |
| **ranking-service** | — | No existe aún |

---

## Diagrama de arquitectura (Containers)

```mermaid
flowchart LR
  FE[Frontend / Client]
  BFF[BFF Service\nAPI pública /api]

  JS[jugador-service]
  DS[desafio-service]
  IS[ingame-service]
  RS[ranking-service]
  AS[auth-service]

  FE -->|REST /api| BFF

  BFF -->|REST interno| JS
  BFF -->|REST interno| DS
  BFF -->|REST interno| IS
  BFF -->|REST interno| RS
  BFF -->|REST interno| AS

 
```

---

## Autenticación y seguridad

### Mecanismo JWT (RS256)

- **auth-service** emite tokens firmados con `private.pem` (RS256, Nimbus).
- **bff-service** y demás microservicios validan tokens usando el endpoint JWKS dinámico del auth-service.
- La clave pública se descarga una sola vez y se cachea. Solo se re-descarga si aparece un `kid` desconocido (permite rotación de claves sin redesploy).
- El auto-config centralizado vive en `gwent-security` (`GwentJwtDecoderAutoConfig`) y se activa con la propiedad `gwent.security.jwt.jwks-uri`.

### Flujo: registro de usuario

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AS as auth-service :8085
    participant JS as jugador-service :8082
    participant DBA as MySQL (gw_user)
    participant DBJ as MySQL (gw_jugador)

    C->>BFF: POST /api/v1/auth/register<br/>{username, password, email, gameId, tag}
    BFF->>AS: POST /auth/v1/register
    AS->>DBA: Verifica unicidad (username, email, gameId#tag)
    DBA-->>AS: OK
    AS->>DBA: INSERT gw_user (UUID generado en PrePersist)
    DBA-->>AS: usuario creado
    AS-->>BFF: { accessToken, tokenType, expiresIn }
    BFF->>JS: POST /api/v1/players {apodo=username}<br/>Authorization: Bearer <accessToken>
    JS->>JS: Valida JWT → extrae userId del token
    JS->>DBJ: INSERT gw_jugador (userId, apodo, nivel=1, stats=0)
    DBJ-->>JS: perfil creado
    JS-->>BFF: PlayerProfileDTO
    BFF-->>C: { accessToken, tokenType, expiresIn }
```

### Flujo: login y obtención del JWT

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AS as auth-service :8085
    participant DB as MySQL (gw_user)

    C->>BFF: POST /api/v1/auth/login<br/>{identifier, password}
    note over BFF: identifier puede ser email,<br/>gameId#tag o username
    BFF->>AS: POST /auth/v1/login (proxy)
    AS->>DB: Busca usuario por identifier
    DB-->>AS: UserEntity
    AS->>AS: Verifica password (BCrypt)
    AS->>AS: Firma JWT RS256<br/>claims: sub=userId, userId,<br/>username, gameId, tag<br/>TTL: 900s
    AS-->>BFF: { token, expiresInSeconds }
    BFF-->>C: { token, expiresInSeconds }
```

### Flujo: request autenticado (BFF → microservicio interno)

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant GS as gwent-security<br/>(NimbusJwtDecoder)
    participant JWKS as auth-service<br/>/.well-known/jwks.json
    participant MS as microservicio interno

    C->>BFF: GET /api/algo<br/>Authorization: Bearer <token>
    BFF->>GS: Valida JWT
    alt clave en cache
        GS-->>BFF: JWT válido → Actor extraído
    else kid desconocido / primer arranque
        GS->>JWKS: GET http://localhost:8085/.well-known/jwks.json
        JWKS-->>GS: { keys: [RSA public key] }
        GS->>GS: Cachea clave
        GS-->>BFF: JWT válido → Actor extraído
    end
    BFF->>MS: llamada interna<br/>Authorization: Bearer <token> (propagado por AuthForwardingInterceptor)
    MS->>MS: Valida JWT con su propio NimbusJwtDecoder<br/>(mismo JWKS endpoint)
    MS-->>BFF: respuesta
    BFF-->>C: respuesta
```

---

## Gestión de identidad (regla base)

- El **actorPlayerId** se obtiene desde el token en el BFF.
- El cliente **no envía playerId del actor**.
- El BFF propaga el Bearer token completo a los microservicios internos (via `AuthForwardingInterceptor`).
- Cada microservicio reconstruye el `Actor` del JWT con su propio `ActorResolver`.

JWT claims:
```json
{
  "sub": "uuid-del-usuario",
  "userId": "uuid-del-usuario",
  "username": "JohnDoe",
  "gameId": "player99",
  "tag": "1234"
}
```

---

# API pública (BFF)

Todos los endpoints expuestos al frontend viven bajo `/api`.

---

## 0. Autenticación

### Registro
```
POST /api/v1/auth/register
Body: { "username": "string", "password": "string", "email": "string", "gameId": "string", "tag": "string" }
```
Crea usuario en auth-service y perfil en jugador-service. Devuelve JWT.

### Login
```
POST /api/v1/auth/login
Body: { "identifier": "string", "password": "string" }
```
`identifier` puede ser `username`, `email` o `gameId#tag`. Devuelve JWT.

### Cambiar contraseña
```
PATCH /api/v1/auth/password
Authorization: Bearer <token>
Body: { "currentPassword": "string", "newPassword": "string" }
```
Requiere JWT. Verifica la contraseña actual antes de actualizar.

---

## 1. Jugadores

El perfil de jugador se crea automáticamente en `jugador-service` cuando el usuario se registra (ver flujo de registro). La entidad `JugadorEntity` vive en jugador-service y está vinculada al `userId` de auth-service.

### Campos del perfil de jugador (`gw_jugador`)
| Campo | Tipo | Descripción |
|---|---|---|
| `userId` | UUID (PK) | Igual al `userId` de `UserEntity` en auth-service |
| `apodo` | String (unique) | Nombre de pantalla. Inicialmente = `username` del registro |
| `avatarUrl` | String | Opcional |
| `nivel` | int | Empieza en 1 |
| `victorias` | int | Contador de victorias |
| `derrotas` | int | Contador de derrotas |
| `empates` | int | Contador de empates |
| `createdAt` | LocalDateTime | Timestamp de creación |

### Endpoint interno: crear perfil
```
POST /api/v1/players          (jugador-service interno, puerto 8082)
Authorization: Bearer <token>
Body: { "apodo": "string" }
```
Llamado por el BFF durante el registro. El `userId` se extrae del JWT.

---

### Obtener mi perfil ✅
```
GET /api/players/me
Authorization: Bearer <token>
```
Devuelve el perfil del jugador autenticado. Si el perfil no existe aún, se crea automáticamente (lazy creation).

### Actualizar mi perfil ✅
```
PATCH /api/players/me
Authorization: Bearer <token>
Body: { "apodo": "string", "avatarUrl": "string" }
```
Ambos campos son opcionales. Devuelve el perfil actualizado.

### Obtener perfil público de otro jugador
```
GET /api/players/{playerId}
```
**TO DO** — Implementar en jugador-service y exponer vía BFF.

### Eliminar mi perfil
```
DELETE /api/players/me
```
**TO DO** — Verificar partidas activas antes de eliminar.

---

## 2. Cartas

### Listar cartas del catálogo ✅
```
GET /api/v1/cards
GET /api/v1/cards?faccion=REINO_DEL_NORTE&fila=ASEDIO
GET /api/v1/cards?esEspecial=true
GET /api/v1/cards?esHeroe=true&habilidad=MEDICO
Authorization: Bearer <token>
```
Devuelve el catálogo global de cartas. Todos los filtros son opcionales y combinables entre sí:

| Param | Tipo | Valores |
|---|---|---|
| `faccion` | enum | `REINO_DEL_NORTE`, `NILFGAARD`, `MONSTRUOS`, `SCOIA_TAEL`, `SKELLIGE`, `NEUTRAL` |
| `fila` | enum | `CUERPO_A_CUERPO`, `DISTANCIA`, `ASEDIO`, `AGIL` |
| `tipo` | enum | `UNIDAD`, `CLIMA`, `ESPECIAL` |
| `esHeroe` | boolean | `true` / `false` |
| `habilidad` | enum | `NINGUNA`, `ESPIA`, `MEDICO`, `ENLACE_APRETADO`, `REFUERZO_MORAL`, `DECOY`, `COMANDANTE_INVOCACION`, `ESCUDO_IMPENETRABLE`, `VÍNCULO_ESTRECHO`, `MUSTER` |
| `esEspecial` | boolean | `true` → retorna tipo `CLIMA` + `ESPECIAL`. Tiene precedencia sobre `tipo`. |

### Obtener carta por ID ✅
```
GET /api/v1/cards/{id}
Authorization: Bearer <token>
```
Devuelve una carta específica del catálogo.

### Campos de la carta (`gw_carta_catalogo`)
| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long (PK) | Autoincremental |
| `nombre` | String | Nombre de la carta |
| `faccion` | Enum | `REINO_DEL_NORTE`, `NILFGAARD`, `MONSTRUOS`, `SCOIA_TAEL`, `SKELLIGE`, `NEUTRAL` |
| `tipo` | Enum | `UNIDAD`, `CLIMA`, `ESPECIAL` |
| `fila` | Enum (nullable) | `CUERPO_A_CUERPO`, `DISTANCIA`, `ASEDIO`, `AGIL` |
| `fuerza` | Integer (nullable) | Fuerza de la unidad |
| `habilidad` | Enum | `NINGUNA`, `ESPIA`, `MEDICO`, `ENLACE_APRETADO`, `REFUERZO_MORAL`, `DECOY`, `COMANDANTE_INVOCACION`, `ESCUDO_IMPENETRABLE`, `VÍNCULO_ESTRECHO`, `MUSTER` |
| `esHeroe` | boolean | Las cartas héroe no se ven afectadas por efectos de clima |
| `imagenUrl` | String (nullable) | URL de la imagen |
| `createdAt` | LocalDateTime | Timestamp de creación |
| `modifiedAt` | LocalDateTime (nullable) | Última modificación |
| `deletedAt` | LocalDateTime (nullable) | Soft delete |

---

## 3. Desbloqueo de cartas

### Desbloquear múltiples cartas ✅
```
POST /api/v1/players/me/cards
Authorization: Bearer <token>
Body: { "cardIds": [1, 2, 3] }
```
Desbloquea una o más cartas del catálogo para el jugador autenticado. Las cartas ya desbloqueadas se omiten silenciosamente (idempotente). Devuelve solo las cartas **recién** desbloqueadas.

### Listar mis cartas desbloqueadas ✅
```
GET /api/v1/players/me/cards
Authorization: Bearer <token>
```
Devuelve todas las cartas desbloqueadas del jugador autenticado, incluyendo los datos completos de cada carta del catálogo y la fecha de desbloqueo.

### Campos de la respuesta (`CartaJugadorDTO`)
| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long | ID del registro de desbloqueo |
| `cantidad` | int | Copias que posee el jugador |
| `unlockedAt` | LocalDateTime | Fecha y hora del primer desbloqueo |
| `carta` | CartaCatalogoDTO | Datos completos de la carta (incluye `maxCopias`) |

---

## 4. Mazos

### Listar mazos
```
GET /api/players/{playerId}/decks
```

### Crear mazo
```
POST /api/players/{playerId}/decks
```

### Editar mazo
```
PATCH /api/decks/{deckId}
```

### Eliminar mazo
```
DELETE /api/decks/{deckId}
```

### Cartas del mazo
```
GET    /api/decks/{deckId}/cards
POST   /api/decks/{deckId}/cards
DELETE /api/decks/{deckId}/cards/{cardId}
```

**TO DO (mazos)**
- Límite de 3 mazos
- Validar cartas desbloqueadas
- Reglas de construcción

---

## 5. Desafíos

### Enviar desafío
```
POST /api/challenges
```

### Inbox / Outbox
```
GET /api/challenges/inbox
GET /api/challenges/outbox
```

### Aceptar desafío
```
POST /api/challenges/{challengeId}/accept
```

### Rechazar desafío
```
POST /api/challenges/{challengeId}/reject
```

### Cancelar desafío
```
POST /api/challenges/{challengeId}/cancel
```

**TO DO (desafíos)**
- Expiración
- Evitar duplicados

---

## Flujo: aceptar desafío → crear partida

```mermaid
sequenceDiagram
    autonumber
    Frontend->>BFF: POST /api/challenges/{id}/accept
    BFF->>desafio-service: acceptChallenge(actor)
    desafio-service-->>BFF: challenge ACCEPTED
    BFF->>ingame-service: POST /matches
    ingame-service-->>BFF: matchId
    BFF-->>Frontend: { matchId }
```

---

## 6. Partidas

### Listar partidas
```
GET /api/matches
```

### Obtener estado de partida
```
GET /api/matches/{matchId}
```

### Jugar carta
```
POST /api/matches/{matchId}/play-card
```

### Pasar turno / ronda
```
POST /api/matches/{matchId}/pass
```

---

## Flujo: acciones in-game

```mermaid
sequenceDiagram
    autonumber
    Frontend->>BFF: GET /api/matches/{matchId}
    BFF->>ingame-service: GET /matches/{matchId}/state (actor)
    ingame-service-->>BFF: state
    BFF-->>Frontend: state

    Frontend->>BFF: POST /api/matches/{matchId}/play-card
    BFF->>ingame-service: play-card(actor)
    ingame-service-->>BFF: stateVersion
    BFF-->>Frontend: stateVersion

    Frontend->>BFF: POST /api/matches/{matchId}/pass
    BFF->>ingame-service: pass(actor)
    ingame-service-->>BFF: stateVersion
    BFF-->>Frontend: stateVersion
```

---

## Manejo de errores

Todos los servicios devuelven errores con la estructura `ErrorDTO`:

```json
{
  "serviceOrigin": "jugador-service",
  "status": 404,
  "message": "Player not found",
  "type": null,
  "path": null,
  "body": null
}
```

El BFF propaga el `ErrorDTO` del servicio interno al cliente con el mismo código HTTP. Los errores de transporte (servicio caído) se traducen a `502 Bad Gateway`.

---

## Próximos pasos

- Mulligan inicial
- Reglas de rondas (best of 3)
- Cartas especiales (clima, héroes)
- Ranking y estadísticas
- Eventos async

---

**Este README define el contrato de la API y la arquitectura base del proyecto.**

