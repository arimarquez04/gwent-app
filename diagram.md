# Ms-Gwent — Diagramas de flujo de negocio

Cada sección describe una acción del usuario, el endpoint del BFF que debe invocar, y las llamadas internas que se realizan.

---

## 1. Registro de usuario

**Acción:** El usuario crea una cuenta por primera vez.

```
POST /api/v1/auth/register
Body: { username, password, email, gameId, tag }
```

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AS as auth-service :8085
    participant JS as jugador-service :8082
    participant DBA as MySQL (gw_user)
    participant DBJ as MySQL (gw_jugador)

    C->>BFF: POST /api/v1/auth/register
    BFF->>AS: POST /auth/v1/register { username, password, email, gameId, tag }
    AS->>DBA: Verifica unicidad (username, email, gameId#tag)
    DBA-->>AS: OK
    AS->>DBA: INSERT gw_user (userId=UUID, hash de password)
    DBA-->>AS: usuario creado
    AS-->>BFF: GenericResponseDTO { accessToken, tokenType, expiresIn }
    BFF->>JS: POST /api/v1/players { apodo=username }<br/>Authorization: Bearer <accessToken>
    JS->>JS: Valida JWT → extrae userId del token
    JS->>DBJ: INSERT gw_jugador (userId, apodo, nivel=1, stats=0)
    DBJ-->>JS: perfil creado
    JS-->>BFF: GenericResponseDTO { PlayerProfileDTO }
    BFF-->>C: GenericResponseDTO { accessToken, tokenType, expiresIn }
```

**Errores posibles:**
- `409 Conflict` — username, email o gameId#tag ya en uso

---

## 2. Login

**Acción:** El usuario inicia sesión y obtiene un JWT.

```
POST /api/v1/auth/login
Body: { identifier, password }
```
> `identifier` puede ser `username`, `email` o `gameId#tag`

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AS as auth-service :8085
    participant DB as MySQL (gw_user)

    C->>BFF: POST /api/v1/auth/login { identifier, password }
    BFF->>AS: POST /auth/v1/login
    AS->>DB: Busca usuario por identifier
    DB-->>AS: UserEntity
    AS->>AS: Verifica password (BCrypt)
    AS->>AS: Firma JWT RS256<br/>claims: sub=userId, username, gameId, tag<br/>TTL: 900s
    AS-->>BFF: GenericResponseDTO { accessToken, tokenType, expiresIn }
    BFF-->>C: GenericResponseDTO { accessToken, tokenType, expiresIn }
```

**Errores posibles:**
- `401 Unauthorized` — credenciales incorrectas

---

## 3. Obtener mi perfil de jugador

**Acción:** El usuario autenticado consulta su propio perfil.

```
GET /api/players/me
Authorization: Bearer <token>
```

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant JS as jugador-service :8082
    participant DB as MySQL (gw_jugador)

    C->>BFF: GET /api/players/me
    BFF->>JS: GET /api/v1/players/me (Bearer propagado)
    JS->>JS: Valida JWT → extrae userId
    JS->>DB: SELECT gw_jugador WHERE userId = ?

    alt Perfil existe
        DB-->>JS: JugadorEntity
        JS-->>BFF: GenericResponseDTO { PlayerProfileDTO }
        BFF-->>C: GenericResponseDTO { PlayerProfileDTO }
    else Perfil no existe (404)
        JS-->>BFF: ErrorDTO { status: 404 }
        BFF->>JS: POST /api/v1/players { apodo=username } (lazy creation)
        JS->>DB: INSERT gw_jugador
        DB-->>JS: perfil creado
        BFF->>JS: GET /api/v1/players/me (reintento)
        JS-->>BFF: GenericResponseDTO { PlayerProfileDTO }
        BFF-->>C: GenericResponseDTO { PlayerProfileDTO }
    end
```

**Nota:** La creación lazy ocurre cuando el registro falló silenciosamente en jugador-service pero el usuario ya existe en auth-service.

---

## 4. Actualizar mi perfil de jugador

**Acción:** El usuario cambia su apodo o avatar.

```
PATCH /api/players/me
Authorization: Bearer <token>
Body: { apodo?, avatarUrl? }
```

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant JS as jugador-service :8082
    participant DB as MySQL (gw_jugador)

    C->>BFF: PATCH /api/players/me { apodo?, avatarUrl? }
    BFF->>JS: PATCH /api/v1/players/me (Bearer propagado)
    JS->>JS: Valida JWT → extrae userId
    JS->>DB: SELECT gw_jugador WHERE userId = ?
    DB-->>JS: JugadorEntity
    JS->>JS: Aplica cambios (solo campos no nulos)
    JS->>DB: UPDATE gw_jugador
    DB-->>JS: ok
    JS-->>BFF: GenericResponseDTO { PlayerProfileDTO actualizado }
    BFF-->>C: GenericResponseDTO { PlayerProfileDTO actualizado }
```

**Errores posibles:**
- `404 Not Found` — perfil no existe
- `409 Conflict` — el apodo ya está en uso por otro jugador

---

## 5. Cambiar contraseña

**Acción:** El usuario autenticado cambia su contraseña.

```
PATCH /api/v1/auth/password
Authorization: Bearer <token>
Body: { currentPassword, newPassword }
```

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AS as auth-service :8085
    participant DB as MySQL (gw_user)

    C->>BFF: PATCH /api/v1/auth/password { currentPassword, newPassword }
    BFF->>BFF: Valida JWT → extrae username del Actor
    BFF->>AS: PATCH /auth/v1/users/me/password<br/>{ username (del JWT), currentPassword, newPassword }
    AS->>DB: SELECT gw_user WHERE username = ?
    DB-->>AS: UserEntity
    AS->>AS: BCrypt.matches(currentPassword, hash)

    alt Contraseña correcta
        AS->>AS: BCrypt.encode(newPassword)
        AS->>DB: UPDATE gw_user SET password = newHash
        DB-->>AS: ok
        AS-->>BFF: GenericResponseDTO { "Password changed successfully" }
        BFF-->>C: GenericResponseDTO { "Password changed successfully" }
    else Contraseña incorrecta
        AS-->>BFF: ErrorDTO { status: 401 }
        BFF-->>C: ErrorDTO { status: 401, message: "Credenciales invalidas" }
    end
```

---

## 6. Flujo de propagación de JWT entre servicios

**Contexto:** Cómo el token viaja del cliente hasta los microservicios internos en cualquier request autenticado.

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant AFI as AuthForwardingInterceptor
    participant MS as Microservicio interno
    participant JWKS as auth-service /.well-known/jwks.json

    C->>BFF: Cualquier endpoint autenticado<br/>Authorization: Bearer <token>
    BFF->>BFF: OAuth2ResourceServer valida JWT<br/>(NimbusJwtDecoder con JWKS cacheado)

    alt Clave pública en cache
        BFF->>BFF: JWT válido → SecurityContext poblado
    else Primera vez / kid desconocido
        BFF->>JWKS: GET http://localhost:8085/.well-known/jwks.json
        JWKS-->>BFF: RSA public key
        BFF->>BFF: Cachea clave, valida JWT
    end

    BFF->>BFF: ActorResolver.currentActor() → Actor { userId, username, gameId, tag }
    BFF->>AFI: RestClient.exchange(...)
    AFI->>AFI: Copia Authorization header del request entrante
    AFI->>MS: Request interno<br/>Authorization: Bearer <token> (mismo token)
    MS->>MS: Valida JWT con su propio NimbusJwtDecoder
    MS->>MS: ActorResolver.currentActor() → Actor
    MS-->>BFF: Respuesta
    BFF-->>C: Respuesta
```

---

## 7. Consultar catálogo de cartas

**Acción:** El usuario autenticado consulta el catálogo de cartas con filtros combinables.

```
GET /api/v1/cards
GET /api/v1/cards?faccion=REINO_DEL_NORTE&fila=ASEDIO
GET /api/v1/cards?esEspecial=true
GET /api/v1/cards?esHeroe=true&habilidad=MEDICO
GET /api/v1/cards/{id}
Authorization: Bearer <token>
```

Filtros disponibles (todos opcionales, combinables): `faccion`, `fila`, `tipo`, `esHeroe`, `habilidad`, `esEspecial` (tipo CLIMA + ESPECIAL, tiene precedencia sobre `tipo`).

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant IS as ingame-service :8083
    participant DB as MySQL (gw_carta_catalogo)

    C->>BFF: GET /api/v1/cards[?filtros]
    BFF->>BFF: Valida JWT
    BFF->>IS: GET /ingame/v1/cards[?filtros] (Bearer propagado)
    IS->>IS: Valida JWT
    IS->>IS: Construye JPA Specification<br/>combinando predicados activos
    IS->>DB: SELECT * WHERE [predicados AND]
    DB-->>IS: List<CartaCatalogo>
    IS-->>BFF: GenericResponseDTO { List<CartaCatalogoDTO> }
    BFF-->>C: GenericResponseDTO { List<CartaCatalogoDTO> }
```

**Errores posibles:**
- `400 Bad Request` — valor de enum inválido (incluye valores permitidos en el mensaje)
- `404 Not Found` — carta no encontrada (solo para `GET /cards/{id}`)

---

## 8. Flujo de errores entre servicios

**Contexto:** Cómo un error en un microservicio interno llega al cliente.

## 8. Desbloquear múltiples cartas

**Acción:** El jugador desbloquea una o más cartas del catálogo en una sola llamada.

```
POST /api/v1/players/me/cards
Authorization: Bearer <token>
Body: { "cardIds": [1, 2, 3] }
```

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant IS as ingame-service :8083
    participant DB as MySQL

    C->>BFF: POST /api/v1/players/me/cards { cardIds: [1,2,3] }
    BFF->>BFF: Valida JWT
    BFF->>IS: POST /ingame/v1/players/me/cards (Bearer propagado)
    IS->>IS: Valida JWT → extrae userId

    IS->>DB: SELECT gw_carta_catalogo WHERE id IN (1,2,3)
    DB-->>IS: 3 cartas encontradas

    IS->>DB: SELECT gw_carta_jugador WHERE jugador_id=? AND carta_catalogo_id IN (1,2,3)
    DB-->>IS: carta 1 ya desbloqueada

    IS->>DB: INSERT gw_carta_jugador (carta 2)
    IS->>DB: INSERT gw_carta_jugador (carta 3)
    DB-->>IS: ok

    IS-->>BFF: GenericResponseDTO { [CartaJugadorDTO x2] } (solo las nuevas)
    BFF-->>C: GenericResponseDTO { [CartaJugadorDTO x2] }
```

**Errores posibles:**
- `404 Not Found` — algún `cardId` no existe en el catálogo

---

## 9. Flujo de errores entre servicios

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant BFF as BFF :8080
    participant GEH as GlobalExceptionHandler (BFF)
    participant MS as Microservicio interno
    participant MEH as GlobalExceptionHandler (MS)

    C->>BFF: Request
    BFF->>MS: Llamada interna (GenericRestInvoker)
    MS->>MEH: Excepción de dominio (ej: ResponseStatusException 404)
    MEH-->>MS: ErrorDTO { serviceOrigin, status, message }
    MS-->>BFF: HTTP 4xx/5xx con body ErrorDTO

    BFF->>BFF: GenericRestInvoker parsea ErrorDTO
    BFF->>BFF: RemoteServiceErrorMapper → InternalService4xxErrorException(errorDTO)
    BFF->>GEH: Lanza excepción
    GEH-->>BFF: Propaga el ErrorDTO original con el mismo HTTP status
    BFF-->>C: HTTP 4xx + ErrorDTO { serviceOrigin, status, message }
```
