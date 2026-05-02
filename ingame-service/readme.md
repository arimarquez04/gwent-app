# ingame-service

Microservicio responsable del catálogo de cartas, cartas desbloqueadas por jugador, mazos y lógica completa de partidas.

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

### Partidas

| Método | Path | Descripción |
|---|---|---|
| `POST` | `/ingame/v1/partidas` | Crear partida |
| `GET` | `/ingame/v1/partidas/{id}` | Ver estado de la partida |
| `POST` | `/ingame/v1/partidas/{id}/mulligan` | Fase de mulligan (intercambiar 0-2 cartas) |
| `POST` | `/ingame/v1/partidas/{id}/jugar-carta` | Jugar carta en una fila |
| `POST` | `/ingame/v1/partidas/{id}/pasar` | Pasar turno (definitivo para la ronda) |
| `POST` | `/ingame/v1/partidas/{id}/usar-lider` | Activar habilidad del líder (una vez por partida) |

---

## Entidades

### `gw_carta_catalogo`
Catálogo global de cartas. Precargado via `seed-data.sql` (41 cartas normales + 18 líderes).

| Campo | Tipo |
|---|---|
| `id` | Long (PK) |
| `nombre` | String |
| `faccion` | Enum: REINO_DEL_NORTE, NILFGAARD, MONSTRUOS, SCOIA_TAEL, SKELLIGE, NEUTRAL |
| `tipo` | Enum: UNIDAD, CLIMA, ESPECIAL, LIDER |
| `fila` | Enum nullable: CUERPO_A_CUERPO, DISTANCIA, ASEDIO, AGIL |
| `fuerza` | Integer nullable |
| `fuerzaTransformada` | Integer nullable — fuerza del Berserker transformado por Mardroeme |
| `habilidad` | Enum: NINGUNA, ESPIA, MEDICO, ENLACE_APRETADO, REFUERZO_MORAL, DECOY, VINCULO_ESTRECHO, MUSTER, CLIMA_LIMPIO, SCORCH, SCORCH_FILA, BERSERKER, MARDROEME, CUERNO_DEL_COMANDANTE, TORMENTA_SKELLIGE, y 18 valores LIDER_* |
| `esHeroe` | boolean — inmune a clima, Horn, MORAL y Scorch |
| `maxCopias` | int |
| `imagenUrl` | String nullable |
| `createdAt` | LocalDateTime |

### `gw_carta_jugador`
Cartas desbloqueadas por cada jugador. UniqueConstraint(jugador_id, carta_catalogo_id).

### `gw_mazo`
Mazo de un jugador. Estado: ACTIVO / INACTIVO. Máximo 3 por facción por jugador. Campo `lider` (ManyToOne CartaCatalogo, nullable).

### `gw_mazo_carta`
Relación mazo ↔ carta del catálogo con cantidad. UniqueConstraint(mazo_id, carta_catalogo_id).

### `gw_partida`
| Campo | Descripción |
|---|---|
| `id` | Long (PK) |
| `jugadorUnoId / jugadorDosId` | UUID de cada jugador |
| `mazoJugadorUno / mazoJugadorDos` | ManyToOne Mazo |
| `estado` | MULLIGAN, EN_CURSO, TERMINADA |
| `rondaActual` | int (1-3) |
| `turnoJugadorId` | UUID del jugador con turno |
| `vidasJugadorUno / vidasJugadorDos` | int (0-2) |
| `jugadorUnoPaso / jugadorDosPaso` | boolean — si ya pasó en la ronda |
| `jugadorUnoMulligan / jugadorDosMulligan` | boolean — si ya completó el mulligan |
| `liderUsadoJugadorUno / liderUsadoJugadorDos` | boolean — si la habilidad de líder ya fue activada |
| `cartasReveladasJ1 / cartasReveladasJ2` | String VARCHAR(200) — IDs separados por coma de cartas reveladas por LIDER_REVEAL_HAND |
| `ganadorId` | UUID nullable |
| `empate` | boolean |

### `gw_carta_partida`
Cada instancia de carta dentro de una partida.

| Campo | Descripción |
|---|---|
| `id` | Long (PK) |
| `partida` | ManyToOne Partida |
| `jugadorId` | UUID del propietario actual (puede cambiar con ESPIA/STEAL_GRAVEYARD) |
| `cartaCatalogo` | ManyToOne CartaCatalogo |
| `zona` | ZonaCarta: MAZO, MANO, CAMPO, CEMENTERIO |
| `filaEnCampo` | FilaCarta nullable — fila donde está en campo |
| `transformado` | boolean — true si el Berserker fue transformado por Mardroeme |
| `esSlotLateral` | boolean — true para Cuerno del Comandante y Mardroeme ESPECIAL en slot lateral |

### `gw_ronda`
Resultado de cada ronda. Campos: `numeroRonda`, `puntajeJugadorUno/Dos`, `ganadorId`, `empate`.

---

## Reglas de negocio (mazos)

- Máx 3 mazos por facción → 409
- Solo cartas de la misma facción + NEUTRAL → 422
- Solo cartas desbloqueadas por el jugador → 422
- Cartas de tipo LIDER no van en `cardEntries`, se asignan en `liderId` → 422
- `liderId: -1` en `PUT` quita el líder
- Para activar: mínimo 22 cartas de tipo UNIDAD → 422
- Al activar un mazo, el mazo ACTIVO previo de esa facción pasa a INACTIVO

## Reglas de negocio (partidas)

- Solo el jugador con turno puede jugar carta o usar líder
- El líder se activa **una vez por partida**; consume turno → 409 si ya usado
- Auto-pass si la mano queda vacía tras jugar
- Si el oponente ya pasó, el jugador mantiene el turno al jugar
- Cuando ambos pasan → resolución de ronda automática
- Líderes auto-activados: LIDER_CANCEL_LEADER al crear partida; LIDER_DRAW_EXTRA al completar mulligan

---

## Habilidades implementadas

| Tipo | Habilidades |
|---|---|
| **Activas (on-play)** | ESPIA, MEDICO, MUSTER, DECOY, CLIMA_LIMPIO, SCORCH, SCORCH_FILA, BERSERKER, MARDROEME |
| **Pasivas (al calcular)** | ENLACE_APRETADO, VINCULO_ESTRECHO, REFUERZO_MORAL, CUERNO_DEL_COMANDANTE |
| **Clima** | NINGUNA (fila única), TORMENTA_SKELLIGE (DISTANCIA + ASEDIO) |
| **Líderes** | 18 habilidades LIDER_* — ver [AbilityRules.md](../AbilityRules.md#habilidades-de-líder) |

---

## Comunicación saliente

ingame-service llama a jugador-service al terminar una partida para actualizar estadísticas.

| Cliente | Destino | Método | URL | Cuándo |
|---|---|---|---|---|
| `JugadorServiceClient` | jugador-service :8082 | POST | `/api/v1/players/match-result` | Al pasar `estado = TERMINADA` en `jugarCarta` o `pasar` |

**Configuración** (`application.yml`):
```yaml
gwent:
  services:
    jugador:
      name: jugador-service
      base-url: http://localhost:8082
      match-result-url: /api/v1/players/match-result
```

La llamada es best-effort: si jugador-service falla, la partida ya fue guardada y el error se loguea como `WARN` sin relanzar la excepción.

---

## Sistema de logs

ingame-service usa SLF4J + Logback con `@Slf4j`. Nivel activo en `application.yml`:

```yaml
logging:
  level:
    com.arimar.gwent.ingameservice.service: DEBUG   # desarrollo
    # com.arimar.gwent.ingameservice.service: INFO  # producción
```

### Qué loguea cada clase

| Clase | Nivel | Contenido |
|---|---|---|
| `PartidaService` | INFO | Partida creada, mulligan, carta jugada, jugador pasa, resultado de ronda (puntos + vidas restantes), partida terminada |
| `PartidaService` | WARN | Fallo al actualizar estadísticas en jugador-service |
| `HabilidadService` | INFO | Habilidad disparada con efecto (ESPIA, MEDICO, MUSTER, DECOY, SCORCH, BERSERKER, MARDROEME) |
| `HabilidadService` | DEBUG | Clima activo detectado, desglose por fila (héroes/no-héroes/factor), contribución individual de cada carta con todos los modificadores aplicados |
| `LiderService` | INFO | Líder activado (habilidad + jugador), efecto ejecutado (cartas afectadas, nombres) |
| `CartaPartidaStateMachine` | DEBUG | Cada transición: `[carta-{id}] '{nombre}': {ORIGEN} → {DESTINO}` |

### Ejemplo de salida DEBUG — cálculo de fuerza

```
DEBUG [CAC] 3 unidad(es) | clima=false horn=true liderDoble=true medioClima=false dobleEspias=false
DEBUG   'Guerrero de la guardia': 4 → 12 (ENLACE×3)
DEBUG   'Guerrero de la guardia': 4 → 12 (ENLACE×3)
DEBUG   'Guerrero de la guardia': 4 → 12 (ENLACE×3)
DEBUG [CAC] héroes=0 no-héroes=36 factor=4 (Horn×2)(Líder×2) → total=144
DEBUG Fuerza total: CAC=144 + DIST=0 + ASEDIO=0 = 144 [liderActivo=LIDER_DOUBLE_CAC dobleEspias=false]
```

---

## Seed data

Ejecutar `src/main/resources/seed-data.sql` manualmente en MySQL antes de usar. Incluye ALTER TABLE para todas las columnas nuevas.
