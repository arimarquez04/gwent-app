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
| **ingame-service** | 8083 | Funcional — catálogo de cartas, desbloqueo, mazos, partidas y habilidades de líder |
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

### Endpoint interno: actualizar estadísticas de partida
```
POST /api/v1/players/match-result    (jugador-service interno, puerto 8082)
Authorization: Bearer <token>
Body: { "jugadorUnoId": "uuid", "jugadorDosId": "uuid", "ganadorId": "uuid|null", "empate": false }
```
Llamado por ingame-service al finalizar cada partida (`estado = TERMINADA`). Incrementa `victorias`, `derrotas` o `empates` en `gw_jugador`. Si jugador-service no está disponible, la partida queda guardada correctamente y el error se registra en el log (best-effort).

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
| `tipo` | enum | `UNIDAD`, `CLIMA`, `ESPECIAL`, `LIDER` |
| `esHeroe` | boolean | `true` / `false` |
| `habilidad` | enum | `NINGUNA`, `ESPIA`, `MEDICO`, `ENLACE_APRETADO`, `REFUERZO_MORAL`, `DECOY`, `VINCULO_ESTRECHO`, `MUSTER`, `CLIMA_LIMPIO`, `SCORCH`, `SCORCH_FILA`, `BERSERKER`, `MARDROEME`, `CUERNO_DEL_COMANDANTE`, `TORMENTA_SKELLIGE`, y 18 valores `LIDER_*` (ver AbilityRules.md) |
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
| `tipo` | Enum | `UNIDAD`, `CLIMA`, `ESPECIAL`, `LIDER` |
| `fila` | Enum (nullable) | `CUERPO_A_CUERPO`, `DISTANCIA`, `ASEDIO`, `AGIL` |
| `fuerza` | Integer (nullable) | Fuerza base de la unidad |
| `fuerzaTransformada` | Integer (nullable) | Fuerza del Berserker transformado (null para las demas cartas) |
| `habilidad` | enum | `NINGUNA`, `ESPIA`, `MEDICO`, `ENLACE_APRETADO`, `REFUERZO_MORAL`, `DECOY`, `VINCULO_ESTRECHO`, `MUSTER`, `CLIMA_LIMPIO`, `SCORCH`, `SCORCH_FILA`, `BERSERKER`, `MARDROEME`, `CUERNO_DEL_COMANDANTE`, `TORMENTA_SKELLIGE`, y 18 valores `LIDER_*` (ver AbilityRules.md) |
| `esHeroe` | boolean | Héroes inmunes a todos los efectos externos: clima, Horn, MORAL, Scorch |
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

## 4. Mazos ✅

Los mazos pertenecen a una facción específica y contienen cartas desbloqueadas por el jugador. Un mazo puede tener un líder (carta de tipo `LIDER`) y un conjunto de cartas de unidad/especial/clima de su facción o NEUTRAL.

**Reglas de negocio:**
- Máximo **3 mazos por facción** por jugador (1 activo + 2 inactivos)
- Se necesitan al menos **22 cartas de tipo UNIDAD** para poder **activar** un mazo
- Solo cartas de la **misma facción** o **NEUTRAL**
- Solo cartas que el jugador tenga **desbloqueadas**
- Las cartas **no quedan bloqueadas** al mazo — pueden estar en múltiples mazos simultáneamente
- **1 líder** por mazo (campo separado, opcional al crear/guardar, requerido para jugar)
- Solo puede haber **1 mazo activo** por facción; al activar otro, el anterior queda INACTIVO

### Crear mazo ✅
```
POST /api/v1/players/me/mazos
Authorization: Bearer <token>
Body: { "nombre": "string", "faccion": "REINO_DEL_NORTE", "liderId": 32, "cardEntries": [{ "cartaCatalogoId": 5, "cantidad": 1 }] }
```
`liderId` y `cardEntries` son opcionales al crear.

**Errores:** `409` si ya existen 3 mazos de la misma facción · `422` si carta es de otra facción · `422` si carta no desbloqueada · `422` si se pasa un líder en `cardEntries`

### Listar mis mazos ✅
```
GET /api/v1/players/me/mazos
Authorization: Bearer <token>
```

### Obtener mazo por ID ✅
```
GET /api/v1/players/me/mazos/{id}
Authorization: Bearer <token>
```

### Editar mazo ✅
```
PUT /api/v1/players/me/mazos/{id}
Authorization: Bearer <token>
Body: { "nombre": "string", "liderId": 33, "cardEntries": [...] }
```
Todos los campos son opcionales. `liderId: null` → no cambia el líder · `liderId: -1` → quita el líder. `cardEntries` reemplaza todas las cartas del mazo si se envía.

### Activar mazo ✅
```
PATCH /api/v1/players/me/mazos/{id}/activate
Authorization: Bearer <token>
```
Activa el mazo dentro de su facción. El mazo activo anterior de esa facción pasa a INACTIVO.

**Errores:** `422` si el mazo tiene menos de 22 unidades.

### Eliminar mazo ✅
```
DELETE /api/v1/players/me/mazos/{id}
Authorization: Bearer <token>
```
Responde `204 No Content`. Las `MazoCarta` se eliminan en cascada.

### Campos de la respuesta (`MazoDTO`)
| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long | ID del mazo |
| `nombre` | String | Nombre del mazo |
| `faccion` | String | Facción del mazo |
| `estado` | String | `ACTIVO` / `INACTIVO` |
| `lider` | CartaCatalogoDTO (nullable) | Carta líder asignada |
| `cartas` | List\<MazoCartaDTO\> | Cartas del mazo con cantidad |
| `totalUnidades` | int | Total de unidades (suma de `cantidad` para tipo UNIDAD) |
| `createdAt` | LocalDateTime | Timestamp de creación |
| `modifiedAt` | LocalDateTime (nullable) | Última modificación |

### Cartas líder

Hay 18 líderes en total (4 por facción para REINO_DEL_NORTE, NILFGAARD, MONSTRUOS y SCOIA_TAEL; 2 para SKELLIGE). Son cartas del catálogo con `tipo=LIDER`, sin fila ni fuerza, `esHeroe=true`, `maxCopias=1`. Se asignan al mazo por `liderId` (ID de `gw_carta_catalogo`). El jugador debe haberlos desbloqueado previamente.

Cada líder tiene una habilidad `LIDER_*` única que puede activarse **una vez por partida** durante el propio turno (consume turno). Ver [AbilityRules.md](AbilityRules.md#habilidades-de-líder) para el detalle completo de cada habilidad.

### Datos de seed (`seed-data.sql`)
Incluye 41 cartas normales y 18 líderes. Incluye ALTER TABLE para columnas nuevas. Ejecutar manualmente en MySQL antes de usar.

---

## 5. Desafíos

**TO DO** — Pendiente de implementar en `solicitud-service`.

Endpoints planeados:
```
POST /api/challenges                       → Enviar desafío
GET  /api/challenges/inbox                 → Desafíos recibidos
GET  /api/challenges/outbox                → Desafíos enviados
POST /api/challenges/{id}/accept           → Aceptar desafío
POST /api/challenges/{id}/reject           → Rechazar desafío
POST /api/challenges/{id}/cancel           → Cancelar desafío
```

---

## 6. Partidas ✅

Lógica de partidas implementada en `ingame-service`. Dos jugadores se enfrentan con mazos activos en formato **best-of-3** rondas. Cada jugador empieza con **2 vidas** y pierde 1 al perder o empatar una ronda.

> **Alcance actual:** Cartas `UNIDAD`, `CLIMA` y `ESPECIAL` son jugables. Habilidades activas: ESPIA, MEDICO, MUSTER, DECOY, CLIMA_LIMPIO, SCORCH, SCORCH_FILA, BERSERKER, MARDROEME. Habilidades pasivas: ENLACE_APRETADO, VINCULO_ESTRECHO, REFUERZO_MORAL, CUERNO_DEL_COMANDANTE. Clima: Tormenta de escarcha, Lluvia ácida, Niebla espesa, Tormenta de Skellige (doble fila). Habilidades de líder: 18 implementadas (ver [AbilityRules.md](AbilityRules.md#habilidades-de-líder)). Los héroes son inmunes a clima, Horn, MORAL y Scorch.

### Endpoints internos (`ingame-service :8083`)

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/ingame/v1/partidas` | Crear partida |
| `GET` | `/ingame/v1/partidas/{id}` | Ver estado de la partida |
| `POST` | `/ingame/v1/partidas/{id}/mulligan` | Fase de mulligan (intercambiar 0-2 cartas) |
| `POST` | `/ingame/v1/partidas/{id}/jugar-carta` | Jugar carta en una fila |
| `POST` | `/ingame/v1/partidas/{id}/pasar` | Pasar turno (definitivo para la ronda) |
| `POST` | `/ingame/v1/partidas/{id}/usar-lider` | Activar habilidad del líder (una vez por partida) |

### Flujo del juego

```
CREAR PARTIDA → Repartir 10 cartas → [Auto: LIDER_CANCEL_LEADER si aplica]
  → MULLIGAN → [Auto: LIDER_DRAW_EXTRA si aplica] → EN_CURSO (Ronda 1)
  → Jugar carta / Usar líder / Pasar → Ambos pasan → Resolver ronda
  → ¿Fin? → TERMINADA  |  → Robar cartas → Siguiente ronda
```

### Crear partida
```
POST /ingame/v1/partidas
Authorization: Bearer <token>
Body: { "oponenteId": "uuid", "mazoId": 5, "mazoOponenteId": 8 }
```
Ambos mazos deben estar `ACTIVO`. Se reparten 10 cartas aleatorias a cada jugador. Estado inicial: `MULLIGAN`. Turno inicial: aleatorio.

> `mazoOponenteId` es temporal para testing. Será reemplazado cuando `solicitud-service` gestione la aceptación del desafío.

### Mulligan
```
POST /ingame/v1/partidas/{id}/mulligan
Authorization: Bearer <token>
Body: { "cartaPartidaIds": [101, 105] }
```
Cada jugador puede intercambiar **0 a 2** cartas de su mano por cartas aleatorias del mazo. Array vacío = skip. Cuando ambos completan el mulligan → estado cambia a `EN_CURSO`, ronda 1.

### Jugar carta
```
POST /ingame/v1/partidas/{id}/jugar-carta
Authorization: Bearer <token>
Body: {
  "cartaPartidaId": 101,
  "fila": "CUERPO_A_CUERPO",      ← requerido para cartas AGIL
  "reviveCartaId": 55,             ← solo MEDICO: carta del cementerio a revivir
  "reviveFila": "DISTANCIA",       ← solo si la carta a revivir es AGIL
  "targetCartaId": 72              ← solo DECOY: unidad en campo a devolver a la mano
}
```
Solo el jugador con turno puede jugar. Cartas `UNIDAD`, `CLIMA` y `ESPECIAL` son jugables. La fila se determina automáticamente según la carta; cartas `AGIL` requieren `fila` en el request. Cartas de slot lateral (Cuerno del Comandante y Mardroeme ESPECIAL) requieren `fila`. Si el oponente ya pasó, el jugador mantiene el turno. Auto-pass si la mano queda vacía.

**Efectos de habilidades al jugar:**
- `ESPIA` — la carta va al campo del oponente; el jugador roba 2 cartas del mazo
- `MEDICO` — si se envía `reviveCartaId`, esa unidad no-héroe del cementerio pasa al campo; `reviveFila` requerido si la carta a revivir es AGIL
- `MUSTER` — todas las copias del mismo tipo en el mazo pasan automáticamente al campo
- `DECOY` — la unidad `targetCartaId` del campo regresa a la mano; DECOY ocupa su fila
- `CLIMA_LIMPIO` — elimina todas las cartas CLIMA del campo (ambos jugadores) y se descarta
- `SCORCH` (ESPECIAL) — destruye la(s) unidad(es) con mayor fuerza de ambos campos si total ≥ 10, luego se descarta
- `SCORCH_FILA` (UNIDAD) — destruye la(s) unidad(es) enemigas más fuertes de la propia fila si total enemigo ≥ 10
- `BERSERKER` — se transforma (usa `fuerzaTransformada`) si hay Mardroeme en la fila
- `MARDROEME` — transforma todos los Berserkers de la fila; como ESPECIAL ocupa el slot lateral
- `CUERNO_DEL_COMANDANTE` (ESPECIAL) — ocupa el slot lateral de la fila elegida; dobla no-héroes pasivamente

### Pasar turno
```
POST /ingame/v1/partidas/{id}/pasar
Authorization: Bearer <token>
```
El jugador deja de jugar cartas por el resto de la ronda. Cuando ambos han pasado se resuelve la ronda.

### Resolución de ronda

Se calcula la fuerza de cada jugador aplicando modificadores en orden de precedencia:

1. **Héroes** — siempre valen su fuerza base (inmunes a todo efecto externo)
2. **Clima activo** — no-héroes en filas afectadas valen 1 (o `ceil(fuerza/2)` con LIDER_HALF_WEATHER)
3. **ENLACE_APRETADO / VINCULO_ESTRECHO** — multiplica la fuerza por el número de copias en la fila
4. **REFUERZO_MORAL** — +1 a no-héroes por cada carta MORAL presente (excepto a sí misma)
5. **CUERNO_DEL_COMANDANTE** — dobla la suma de no-héroes de la fila (si no hay clima); héroes no se doblan
6. **Líderes pasivos** — DOUBLE_CAC/RANGED/SIEGE multiplica la fila correspondiente; DOUBLE_SPIES dobla ESPIA en ambos campos

Mayor fuerza gana; empate = ambos pierden 1 vida. Todas las cartas del campo van al cementerio. Entre rondas se roban cartas del mazo: **2** tras ronda 1, **1** tras ronda 2. El ganador de la ronda va primero en la siguiente.

### Activar habilidad de líder
```
POST /ingame/v1/partidas/{id}/usar-lider
Authorization: Bearer <token>
Body: { "targetCartaPartidaId": 123, "descartarIds": [45, 67] }
```
Una sola activación por partida; consume el turno. Los campos del body son opcionales según la habilidad. Ver [AbilityRules.md](AbilityRules.md#habilidades-de-líder) para el detalle de cada líder.

### Vista asimétrica (`PartidaDTO`)
Cada jugador ve su propia mano pero **no** la del oponente (solo el count). Tableros y cementerios de ambos son visibles.

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | Long | ID de la partida |
| `estado` | Enum | `MULLIGAN`, `EN_CURSO`, `TERMINADA` |
| `rondaActual` | int | Número de ronda (1-3) |
| `esMiTurno` | boolean | Si es el turno del jugador autenticado |
| `climaEnCampo` | List\<CartaPartidaDTO\> | Cartas CLIMA actualmente en el campo |
| `yo` | JugadorPartidaDTO | Estado propio (mano, tablero, cementerio, vidas, líder) |
| `oponente` | JugadorPartidaDTO | Estado del oponente (mano=null, manoCount, tablero, cementerio, líder) |
| `rondas` | List\<RondaDTO\> | Historial de rondas con puntajes |
| `ganadorId` | UUID (nullable) | Ganador de la partida |
| `empate` | boolean | Si la partida terminó en empate |

**Campos de `JugadorPartidaDTO`:**

| Campo | Tipo | Descripción |
|---|---|---|
| `mano` | List\<CartaPartidaDTO\> | Cartas en mano (null para el oponente) |
| `manoCount` | int | Número de cartas en mano (visible para ambos) |
| `tablero` | TableroDTO | Filas del campo + fuerza + slots laterales |
| `cementerio` | List\<CartaPartidaDTO\> | Cartas descartadas (visible para ambos) |
| `vidas` | int | Vidas restantes (1-2) |
| `paso` | boolean | Si ya pasó en la ronda actual |
| `lider` | CartaCatalogoDTO (nullable) | Líder asignado al mazo |
| `liderUsado` | boolean | Si la habilidad de líder ya fue activada |
| `cartasReveladas` | List\<CartaPartidaDTO\> (nullable) | Solo visible en `oponente` cuando Emhyr Emperador (LIDER_REVEAL_HAND) reveló cartas |

### Entidades de la partida

| Tabla | Descripción |
|---|---|
| `gw_partida` | Partida con 2 jugadores, estado, vidas, turno, resultado. Incluye `lider_usado_jugador_uno/dos` y `cartas_reveladas_j1/j2` para habilidades de líder |
| `gw_carta_partida` | Cada instancia de carta en la partida. Campos: `transformado` (Berserker transformado), `es_slot_lateral` (Cuerno/Mardroeme ESPECIAL en slot) |
| `gw_ronda` | Resultado de cada ronda (puntajes, ganador, empate) |

### Actualización de estadísticas al terminar

Al resolverse la última ronda y pasar la partida a `TERMINADA`, ingame-service llama automáticamente a jugador-service para actualizar las estadísticas de ambos jugadores:

```
ganadorId != null  →  ganador: victorias+1  /  perdedor: derrotas+1
empate = true      →  ambos: empates+1
```

La llamada es **best-effort**: si jugador-service no responde, la partida queda persistida correctamente y el fallo queda registrado en el log de ingame-service como `WARN`.

---

## Sistema de logs

Los servicios usan SLF4J con Logback. El nivel por defecto es `INFO`; el nivel `DEBUG` activa el detalle carta por carta del cálculo de fuerza.

### Configuración de niveles (`application.yml`)

```yaml
logging:
  level:
    com.arimar.gwent.ingameservice.service: DEBUG   # activo en desarrollo
```

### Qué loguea cada nivel

| Nivel | Servicio | Ejemplos |
|---|---|---|
| `INFO` | `PartidaService` | Partida creada, mulligan completo (`→ EN_CURSO`), carta jugada, jugador pasa, resultado de ronda con puntuaciones y vidas, partida terminada con ganador |
| `INFO` | `HabilidadService` | Habilidad disparada (ESPIA, MEDICO, MUSTER…), efecto aplicado, SCORCH total y cartas destruidas |
| `INFO` | `LiderService` | Líder activado, efecto ejecutado (clima eliminado, carta robada del cementerio, cartas reveladas…) |
| `DEBUG` | `HabilidadService` | Clima activo detectado, desglose por fila (`CAC=12 DIST=0 ASEDIO=5`), contribución individual de cada carta con todos sus modificadores |
| `DEBUG` | `CartaPartidaStateMachine` | Cada transición de zona: `[carta-42] 'Guerrero': MAZO → MANO` |
| `WARN` | `PartidaService` | Fallo al actualizar estadísticas en jugador-service (la partida ya fue guardada) |

### Muestra de log con DEBUG activo

```
INFO  [P-5] uuid jugó 'Clan Dimun Pirate' [tipo=UNIDAD, hab=SCORCH, fila=ASEDIO]
INFO  [P-5] SCORCH global: total ambos campos=24 → activa
INFO  [P-5] SCORCH: destruye 2 carta(s) con fuerza efectiva 8 → [Ballestero, Arquero]
DEBUG   [ASEDIO] 3 unidad(es) | clima=false horn=true liderDoble=false ...
DEBUG     'Unidad A': 5 → 5
DEBUG     'Unidad B': 5 → 7 (+MORAL+2)
DEBUG     'Clan Dimun Pirate': 8 → 8
DEBUG   [ASEDIO] héroes=0 no-héroes=18 factor=2 (Horn×2) → total=36
INFO  [P-5] === Ronda 2 === J1=36 pts vs J2=12 pts → J1 gana [liderJ1=LIDER_DOUBLE_CAC usadoJ1=true ...]
INFO  [P-5] J2 pierde 1 vida → vidasJ1=2 vidasJ2=0
INFO  [P-5] Partida TERMINADA — Ganador: J1=uuid...
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

- solicitud-service: ciclo de vida de desafíos (enviar, aceptar, rechazar, cancelar)
- jugador-service: DELETE /api/players/me
- ranking-service: aún no existe

---

**Este README define el contrato de la API y la arquitectura base del proyecto.**

