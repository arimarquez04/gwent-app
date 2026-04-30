# ingame-service

Microservicio responsable del catálogo de cartas, cartas desbloqueadas por jugador y mazos.

**Puerto:** 8083 · **Path base:** `/ingame/v1`

---

## Endpoints implementados

### Catálogo de cartas

| Método | Path | Descripción |
|---|---|---|
| `GET` | `/ingame/v1/cards` | Listar cartas (filtros opcionales) |
| `GET` | `/ingame/v1/cards/{id}` | Obtener carta por ID |

Filtros disponibles para `GET /cards`: `faccion`, `fila`, `tipo`, `esHeroe`, `habilidad`, `esEspecial`

### Cartas del jugador

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/ingame/v1/players/me/cards` | Desbloquear cartas `{ cardIds: [...] }` |
| `GET` | `/ingame/v1/players/me/cards` | Listar cartas desbloqueadas |

### Mazos

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/ingame/v1/players/me/mazos` | Crear mazo (201) |
| `GET` | `/ingame/v1/players/me/mazos` | Listar mis mazos |
| `GET` | `/ingame/v1/players/me/mazos/{id}` | Obtener mazo por ID |
| `PUT` | `/ingame/v1/players/me/mazos/{id}` | Editar mazo |
| `PATCH` | `/ingame/v1/players/me/mazos/{id}/activate` | Activar mazo |
| `DELETE` | `/ingame/v1/players/me/mazos/{id}` | Eliminar mazo (204) |

---

## Entidades

### `gw_carta_catalogo`
Catálogo global de cartas. Precargado via `seed-data.sql` (31 cartas normales + 10 líderes).

| Campo | Tipo |
|---|---|
| `id` | Long (PK) |
| `nombre` | String |
| `faccion` | Enum: REINO_DEL_NORTE, NILFGAARD, MONSTRUOS, SCOIA_TAEL, SKELLIGE, NEUTRAL |
| `tipo` | Enum: UNIDAD, CLIMA, ESPECIAL, LIDER |
| `fila` | Enum nullable: CUERPO_A_CUERPO, DISTANCIA, ASEDIO, AGIL |
| `fuerza` | Integer nullable |
| `habilidad` | Enum: NINGUNA, ESPIA, MEDICO, ENLACE_APRETADO, REFUERZO_MORAL, DECOY, COMANDANTE_INVOCACION, ESCUDO_IMPENETRABLE, MUSTER |
| `esHeroe` | boolean |
| `maxCopias` | int |
| `imagenUrl` | String nullable |
| `createdAt` | LocalDateTime |

### `gw_carta_jugador`
Cartas desbloqueadas por cada jugador. UniqueConstraint(jugador_id, carta_catalogo_id).

### `gw_mazo`
Mazo de un jugador. Estado: ACTIVO / INACTIVO. Máximo 3 por facción por jugador.

### `gw_mazo_carta`
Relación mazo ↔ carta del catálogo con cantidad. UniqueConstraint(mazo_id, carta_catalogo_id).

---

## Reglas de negocio (mazos)

- Máx 3 mazos por facción → 409
- Solo cartas de la misma facción + NEUTRAL → 422
- Solo cartas desbloqueadas por el jugador → 422
- Cartas de tipo LIDER no van en `cardEntries`, se asignan en `liderId` → 422
- `liderId: -1` en `PUT` quita el líder
- Para activar: mínimo 22 cartas de tipo UNIDAD → 422
- Al activar un mazo, el mazo ACTIVO previo de esa facción pasa a INACTIVO

---

## Seed data

Ejecutar `src/main/resources/seed-data.sql` manualmente en MySQL antes de usar.

```sql
-- Opción: solo líderes (si las 31 cartas normales ya están cargadas)
-- Ver las últimas 10 filas del INSERT en seed-data.sql
```
