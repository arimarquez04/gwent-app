# Reglas de habilidades y precedencia — ingame-service

Este documento especifica el orden de evaluación y la precedencia de los efectos de habilidades durante una partida. Sirve como referencia para implementar nuevas habilidades sin romper las existentes.

---

## Índice

1. [Tipos de habilidades](#tipos-de-habilidades)
2. [Habilidades activas (al jugar)](#habilidades-activas-al-jugar)
3. [Habilidades pasivas (al resolver)](#habilidades-pasivas-al-resolver)
4. [Efectos de clima](#efectos-de-clima)
5. [Slot lateral](#slot-lateral)
6. [Habilidades de líder](#habilidades-de-líder)
7. [Orden de precedencia completo](#orden-de-precedencia-completo)
8. [Tabla de interacciones](#tabla-de-interacciones)
9. [Casos límite](#casos-límite)

---

## Tipos de habilidades

| Categoría | Cuándo se evalúa | Ejemplos |
|---|---|---|
| **Activa** | Inmediatamente al jugar la carta (`procesarAlJugar`) | ESPIA, MEDICO, MUSTER, DECOY, CLIMA_LIMPIO, SCORCH, SCORCH_FILA, BERSERKER, MARDROEME |
| **Pasiva** | Al calcular la fuerza (resolución de ronda o vista del tablero) | ENLACE_APRETADO, VINCULO_ESTRECHO, REFUERZO_MORAL, CUERNO_DEL_COMANDANTE |
| **Clima** | Al calcular la fuerza; persiste en campo hasta Buen clima o fin de ronda | Cartas con `tipo = CLIMA` (incluye TORMENTA_SKELLIGE) |
| **Slot lateral** | Persiste en campo como soporte de fila; máximo 1 por fila por jugador | CUERNO_DEL_COMANDANTE (ESPECIAL), MARDROEME (ESPECIAL) |
| **Líder** | Una vez por partida, durante el turno del jugador (consume turno) | Ver sección [Habilidades de líder](#habilidades-de-líder) |

Las cartas con habilidad `NINGUNA` no tienen ningún efecto. Las habilidades de líder se gestionan por separado en `LiderService`.

---

## Habilidades activas (al jugar)

Las habilidades activas se disparan **una sola vez**, en el momento en que la carta pasa de `MANO → CAMPO`. El orden de ejecución dentro de `procesarAlJugar` es el siguiente:

```
1. cartaStateMachine.jugar(carta, fila)   ← carta ya está en CAMPO
2. habilidadService.procesarAlJugar(...)  ← efecto de la habilidad
```

### ESPIA

1. La carta se transfiere al campo del **oponente** (cambia `jugadorId`).
2. El jugador que la jugó **roba 2 cartas** de su mazo (aleatoriamente).
3. La carta aporta su fuerza al oponente, no al jugador que la jugó.
4. Las copias adicionales en el mazo **no se activan** (ESPIA no tiene MUSTER).

### MEDICO

1. El jugador puede especificar `reviveCartaId` en el request.
2. Si `reviveCartaId == null`, el efecto se omite silenciosamente (el médico igual se juega).
3. La carta objetivo debe cumplir todos estos requisitos o la petición falla con 422:
   - Pertenece al mismo jugador.
   - Está en `CEMENTERIO`.
   - No es héroe (`esHeroe = false`).
   - Es de tipo `UNIDAD`.
4. La carta revivida va directamente a `CAMPO` (transición `CEMENTERIO → CAMPO`).
5. Si la carta revivida es AGIL, se requiere `reviveFila` en el request.

### MUSTER

1. Al jugarse una carta con MUSTER, se buscan **todas las copias** de la misma `cartaCatalogoId` que estén en `MAZO` del mismo jugador.
2. Todas pasan automáticamente a `CAMPO` en la misma fila que la carta jugada.
3. Las copias que llegan por MUSTER **no vuelven a disparar** MUSTER (no hay recursión).
4. Las copias que llegan por MUSTER sí tienen sus habilidades **pasivas** (contribuyen a ENLACE, MORAL, etc.).

### DECOY

1. El jugador debe especificar `targetCartaId` (la carta en su propio campo a recuperar).
2. Si `targetCartaId == null`, la petición falla con 422.
3. La carta objetivo debe cumplir todos estos requisitos o la petición falla con 422/404:
   - Pertenece al mismo jugador.
   - Está en `CAMPO`.
   - No es héroe.
4. La carta objetivo regresa a `MANO` (transición `CAMPO → MANO`).
5. El DECOY ocupa la misma fila que tenía la carta objetivo.
6. El DECOY **no aporta fuerza** (su `fuerza` es `NULL` en catálogo).

### CLIMA_LIMPIO (Buen clima)

1. Al jugarse, primero va a `CAMPO` como cualquier carta.
2. Inmediatamente después, `procesarBuenClima` mueve al `CEMENTERIO`:
   - Todas las cartas con `tipo = CLIMA` que estén en `CAMPO` (ambos jugadores).
   - El propio Buen clima.
3. **Buen clima no permanece en campo**.
4. Si no hay clima activo, el efecto no produce error (simplemente descarta Buen clima solo).

### SCORCH

Carta especial instantánea neutral. Efecto global sobre **ambos campos**.

1. Se juega sin fila propia (`fila = null`), va a `CAMPO` momentáneamente.
2. Se calcula la fuerza efectiva de **todas las unidades de ambos campos** combinadas.
3. Si el total combinado `< 10`, el efecto no hace nada.
4. Si `≥ 10`: se identifica la fuerza máxima entre todas las unidades y se destruyen **todas** las que tengan esa fuerza efectiva (sin importar de qué jugador son).
5. La carta Scorch se **descarta a sí misma** al cementerio tras el efecto (no permanece en campo).

> **Riesgo propio:** si tu unidad más fuerte supera a todas las del oponente, Scorch la destruirá a ella.

### SCORCH_FILA

Habilidad de personaje (cartas UNIDAD). Efecto limitado a la fila donde se jugó.

1. La unidad se juega en su fila normalmente.
2. Se calcula la fuerza total de las **unidades enemigas** en esa misma fila, incluyendo:
   - La fuerza de los **héroes** (cuentan para el total pero no pueden ser destruidos).
   - El bonus de **Commander's Horn** si hay uno activo en esa fila del oponente.
3. Si el total enemigo en la fila `< 10`, el efecto no hace nada.
4. Si `≥ 10`: se destruyen todas las unidades enemigas **no-héroe** de esa fila con la mayor fuerza efectiva individual.
5. La carta permanece en campo como unidad normal.

> Nota: `Clan Dimun Pirate` usa SCORCH (global), no SCORCH_FILA.

### BERSERKER

Unidad Skellige que se **transforma** al detectar Mardroeme en su fila.

1. Al jugarse, `procesarBerserker` comprueba si hay alguna carta con habilidad MARDROEME en la misma fila (como unidad o como slot lateral).
2. Si hay Mardroeme → `cartaPartida.transformado = true`. La unidad usa `fuerzaTransformada` en todos los cálculos de fuerza.
3. Si no hay Mardroeme al jugarse, el Berserker espera: si **después** se juega Mardroeme en esa fila, `procesarMardroeme` transformará todos los Berserkers presentes.
4. La transformación es **permanente durante la ronda** (no se revierte si se retira Mardroeme).

| Estado | Fuerza |
|---|---|
| `transformado = false` | `fuerza` del catálogo (6) |
| `transformado = true` | `fuerzaTransformada` del catálogo (8) |

### MARDROEME

Desencadena la transformación de todos los BERSERKER en su fila. Existe en dos formas:

**Como unidad (UNIDAD):**
1. La carta se juega como unidad normal en una fila.
2. `procesarMardroeme` transforma todos los Berserkers en esa misma fila.
3. Permanece en campo como unidad. Los Berserkers que se jueguen **después** en esa fila también se transforman (porque `procesarBerserker` los detecta).

**Como carta de soporte (ESPECIAL, slot lateral):**
1. El jugador elige una fila al jugar (parámetro `fila` en el request).
2. La carta va al slot lateral de esa fila (`esSlotLateral = true`).
3. `procesarMardroeme` transforma todos los Berserkers ya presentes en esa fila.
4. Permanece en el slot lateral hasta el fin de la ronda. Los Berserkers que lleguen después también se transforman.
5. Bloquea el slot de esa fila: no se puede jugar Cuerno del Comandante en la misma fila mientras Mardroeme esté ahí.

---

## Habilidades pasivas (al resolver)

Las habilidades pasivas se evalúan en `HabilidadService.calcularFila()` cada vez que se calcula la fuerza de una fila. **No modifican el estado de las cartas**, solo el valor numérico devuelto.

El orden de evaluación dentro de `calcularFila`:

```
Para cada carta en la fila (solo unidades, sin slot lateral ni CLIMA):
  1. fuerza = getFuerzaEfectiva(cp)           ← usa fuerzaTransformada si transformado=true
  2. Si esHeroe → devuelve fuerza sin modificar ← INMUNE a todo efecto externo
  3. Si hayClima → devuelve 1                 ← clima: máxima prioridad para no-héroes
  4. Si ENLACE o VINCULO:
       fuerza = fuerza × copias_mismo_nombre
  5. fuerza += conteo_MORAL_en_fila (excluye a sí misma)
  → devuelve fuerza individual

Tras sumar toda la fila:
  6. Si tieneHorn && !hayClima:
       total = sumaHeroes + sumaNoHeroes × 2  ← Horn solo dobla no-héroes
```

### ENLACE_APRETADO y VINCULO_ESTRECHO

Ambas habilidades tienen **el mismo efecto mecánico**:

- La fuerza de la carta se **multiplica** por el número de cartas con el mismo `nombre` presentes en la misma fila.
- Ejemplo: 3 "Guerrero de la guardia" (4 de fuerza cada uno) en CAC → cada uno vale `4 × 3 = 12`. Total: 36.
- El conteo incluye la propia carta.
- Si solo hay una copia en campo, el multiplicador es 1 (sin cambio).

### REFUERZO_MORAL

- Cada carta con REFUERZO_MORAL en una fila otorga **+1** a todas las unidades **no-héroe** de esa misma fila, **excepto a sí misma**.
- Si hay múltiples cartas MORAL en la misma fila, el bonus se acumula (+1 por cada una, excluyendo la propia).
- Los héroes **no reciben** el bonus de MORAL.

Ejemplo: fila con [Soldado(5), Soldado(5), Crach an Craite MORAL(9)]:
- Cada Soldado recibe +1 → 6 cada uno.
- Crach no recibe bonus (es héroe). Total: 6 + 6 + 9 = 21.

### CUERNO_DEL_COMANDANTE

Dobla la fuerza total de las **unidades no-héroe propias** en su fila. Los héroes son inmunes y conservan su fuerza sin modificar. Existe en dos formas:

**Como unidad (UNIDAD):**
- Permanece en campo como unidad normal (tiene fuerza propia, contribuye al total).
- El efecto de doblado es pasivo: `tieneHorn()` detecta cualquier unidad con CUERNO_DEL_COMANDANTE en la fila y dobla el total.

**Como carta de soporte (ESPECIAL, slot lateral):**
- El jugador elige la fila al jugar (parámetro `fila` en el request).
- La carta ocupa el slot lateral de esa fila (`esSlotLateral = true`, sin fuerza propia).
- Permanece en campo hasta el fin de la ronda.
- Bloquea el slot: no se puede jugar Mardroeme en la misma fila.
- Máximo **1 por fila por jugador** (sea slot lateral u unidad con CUERNO_DEL_COMANDANTE).

**Reglas de aplicación:**
- El doblado se aplica **sobre el total de la fila** tras aplicar ENLACE, VINCULO y MORAL.
- Si hay **clima activo** en la fila, el clima tiene prioridad: no-héroes valen 1 y el Horn **no dobla** su valor (los héroes sí se doblan, pues son inmunes al clima).
- Solo afecta las unidades propias del jugador, no las del oponente.

---

## Efectos de clima

### Qué hace el clima

Una carta CLIMA activa **reduce a 1** la fuerza de todas las unidades **no-héroe** en la(s) fila(s) afectada(s), para **ambos jugadores simultáneamente**.

| Carta | Fila(s) afectada(s) | Facción |
|---|---|---|
| Tormenta de escarcha | `CUERPO_A_CUERPO` | NEUTRAL |
| Lluvia ácida | `DISTANCIA` | NEUTRAL |
| Niebla espesa | `ASEDIO` | NEUTRAL |
| Tormenta de Skellige | `DISTANCIA` **+** `ASEDIO` | SKELLIGE |

> **Tormenta de Skellige** es la única carta que afecta dos filas simultáneamente. Su habilidad enum es `TORMENTA_SKELLIGE` y `resolverClimaActivo` la trata como caso especial, agregando ambas filas al conjunto activo.

### Inmunidad de héroes

Los héroes (`esHeroe = true`) son **completamente inmunes** a todos los efectos externos. Su fuerza siempre es exactamente la del catálogo (o `fuerzaTransformada` si aplica), sin ninguna modificación:
- Inmunes al clima (no se reducen a 1).
- Inmunes a ENLACE / VINCULO_ESTRECHO (no multiplican por copias).
- Inmunes a REFUERZO_MORAL (no reciben el +1).
- Inmunes a Commander's Horn (su fuerza no se dobla).
- Inmunes a Scorch (no pueden ser destruidos).

### Clima + Commander's Horn

Si el clima está activo en una fila **y** hay Commander's Horn en esa fila:
- Las unidades **no-héroe** valen 1 (clima gana; Horn no las dobla).
- Las unidades **héroe** valen su fuerza base sin modificar (inmunes a clima y a Horn).

### Apilamiento de climas

- Pueden coexistir múltiples climas en distintas filas simultáneamente.
- No hay efecto adicional por apilar dos climas en la misma fila.
- Tormenta de Skellige + Lluvia ácida → DISTANCIA tiene doble clima, pero el resultado es el mismo (no-héroe = 1).

### Persistencia del clima

- Las cartas CLIMA permanecen en `CAMPO` hasta que **Buen clima** las elimine o se resuelva la ronda.
- Al inicio de la ronda siguiente, el campo está vacío → no hay clima activo.
- El clima **no se hereda** entre rondas.

### Tracking del clima activo

El estado del clima se infiere en tiempo real escaneando las cartas con `tipo = CLIMA` en zona `CAMPO`. No existen flags adicionales en `Partida`. Método: `HabilidadService.resolverClimaActivo(campoJ1, campoJ2)`.

---

## Slot lateral

Cada fila tiene un **slot lateral** por jugador. Solo puede estar ocupado por **una** carta de soporte a la vez.

### Cartas que usan slot lateral

| Carta | Habilidad | Efecto |
|---|---|---|
| Cuerno del Comandante (ESPECIAL) | `CUERNO_DEL_COMANDANTE` | Dobla unidades propias de esa fila |
| Mardroeme (ESPECIAL) | `MARDROEME` | Transforma Berserkers de esa fila al entrar, y los que lleguen después |

### Reglas del slot lateral

1. El jugador debe especificar `fila` en el request al jugar una carta de slot lateral.
2. Solo se aceptan `CUERPO_A_CUERPO`, `DISTANCIA` o `ASEDIO` (no `AGIL`).
3. Si la fila ya tiene una carta en el slot lateral, la petición falla con **422**.
4. La carta se almacena en `zona = CAMPO` con `esSlotLateral = true` y `filaEnCampo = fila_elegida`.
5. Las cartas en slot lateral **no aparecen** en las listas de unidades de las filas del `TableroDTO`; aparecen en los campos `slotLateralCuerpoACuerpo`, `slotLateralDistancia`, `slotLateralAsedio`.
6. Las cartas en slot lateral **no se incluyen** en el cálculo de fuerza como unidades (no contribuyen directamente a la suma).
7. Al resolver la ronda, van al cementerio junto con las demás cartas de campo.

---

## Habilidades de líder

Los líderes son cartas de tipo `LIDER` asignadas al mazo. Se activan mediante el endpoint `POST /ingame/v1/partidas/{id}/usar-lider`. La activación consume el turno del jugador (pasa turno al oponente) y solo puede ocurrir una vez por partida (`liderUsado = true` tras activarse).

### Reglas generales

- Solo se puede activar durante el **propio turno** y estando en estado `EN_CURSO`.
- Si el jugador ya pasó la ronda, no puede activar su líder.
- Si el mazo no tiene líder asignado, el endpoint devuelve **422**.
- Intentar activar el líder dos veces devuelve **409**.
- El líder no se juega como carta; permanece fuera del campo y no contribuye a la fuerza.

### Auto-activados (no consumen turno, no se pueden activar manualmente)

| Habilidad enum | Líder | Efecto |
|---|---|---|
| `LIDER_CANCEL_LEADER` | Emhyr: El Blanco Llama | Al **crear la partida**, bloquea permanentemente el líder de **ambos** jugadores (`liderUsadoJ1/J2 = true`). Ninguno podrá usar su líder en toda la partida. |
| `LIDER_DRAW_EXTRA` | Francesca: Flor del Valle | Al completar el **mulligan** (cuando el estado pasa a `EN_CURSO`), el jugador roba 1 carta adicional del mazo automáticamente. |

### Habilidades activas (requieren llamar al endpoint y consumen turno)

#### Eliminar clima

| Habilidad enum | Líder | Efecto |
|---|---|---|
| `LIDER_CLEAR_WEATHER` | Foltest: Señor Comandante del Norte | Elimina todas las cartas CLIMA del campo (ambos jugadores). Idéntico al efecto de Buen clima. |

#### Buscar clima en mazo

| Habilidad enum | Líder | Efecto |
|---|---|---|
| `LIDER_PICK_FOG` | Foltest: Rey de Temeria | Saca "Niebla espesa" del propio mazo y la juega directamente al campo (si existe en el mazo). Sin efecto si no está. |
| `LIDER_PICK_RAIN` | Emhyr: Su Majestad Imperial | Saca "Lluvia acida" del propio mazo y la juega directamente al campo (si existe). |
| `LIDER_PICK_FROST` | Francesca: Elfa de Sangre Pura | Saca "Tormenta de escarcha" del propio mazo y la juega directamente al campo (si existe). |

#### Cementerio

| Habilidad enum | Líder | Efecto | Campo requerido en `UsarLiderRequest` |
|---|---|---|---|
| `LIDER_STEAL_GRAVEYARD` | Emhyr: Señor de las Aguas | Toma una carta del cementerio del **oponente** y la agrega a la propia mano. La carta cambia de propietario (`jugadorId`). | `targetCartaPartidaId` |
| `LIDER_RESTORE_GRAVEYARD` | Eredin: Rey de la Cacería | Toma una carta del **propio** cementerio y la agrega a la propia mano. | `targetCartaPartidaId` |
| `LIDER_SHUFFLE_GRAVEYARDS` | Crach an Craite: Señor de Ard Skellig | Devuelve **todas** las cartas del cementerio de ambos jugadores a sus respectivos mazos. | — |

#### Descartar y robar

| Habilidad enum | Líder | Efecto | Campo requerido |
|---|---|---|---|
| `LIDER_DISCARD_DRAW` | Eredin: Rompenieblas | Descarta exactamente 2 cartas de la propia mano y roba 1 carta aleatoria del propio mazo. Si el mazo está vacío, no se roba ninguna carta (sin error). | `descartarIds` (lista de exactamente 2 IDs) |

#### Destruir unidades enemigas (estilo Scorch por fila)

| Habilidad enum | Líder | Efecto |
|---|---|---|
| `LIDER_SCORCH_CAC` | Francesca: Enid an Gleanna | Si el total del campo enemigo en `CUERPO_A_CUERPO` ≥ 10, destruye la(s) unidad(es) enemigas más fuertes en esa fila. El total incluye la fuerza de los héroes y el bonus de Commander's Horn activo en esa fila. Los héroes no son elegibles para ser destruidos. |
| `LIDER_SCORCH_RANGED` | Foltest: Hijo de Medell | Si el total del campo enemigo en `DISTANCIA` ≥ 10, destruye la(s) unidad(es) enemigas más fuertes. El total incluye la fuerza de los héroes y el bonus de Commander's Horn activo en esa fila. Los héroes no son elegibles para ser destruidos. |

#### Revelar cartas del oponente

| Habilidad enum | Líder | Efecto |
|---|---|---|
| `LIDER_REVEAL_HAND` | Emhyr: Emperador de Nilfgaard | Revela hasta 3 cartas aleatorias de la mano del oponente. Solo son visibles en el `PartidaDTO` del jugador que activó la habilidad, en el campo `oponente.cartasReveladas`. El oponente no ve que sus cartas fueron reveladas. |

### Habilidades pasivas (activar una vez → efecto permanente para la partida)

La activación marca `liderUsado = true`. El efecto se aplica en `calcularFuerzaTotal` y `calcularFila` durante el resto de la partida (mientras `liderUsado = true`).

| Habilidad enum | Líder | Efecto | Aplica a |
|---|---|---|---|
| `LIDER_DOUBLE_CAC` | Eredin: Comandante de los Jinetes Rojos | Dobla la fuerza total de las unidades no-héroe propias en `CUERPO_A_CUERPO`. | Solo el campo propio |
| `LIDER_DOUBLE_RANGED` | Francesca: La Más Hermosa | Dobla la fuerza total de las unidades no-héroe propias en `DISTANCIA`. | Solo el campo propio |
| `LIDER_DOUBLE_SIEGE` | Foltest: El Sitiador | Dobla la fuerza total de las unidades no-héroe propias en `ASEDIO`. | Solo el campo propio |
| `LIDER_DOUBLE_SPIES` | Eredin: El Traicionero | Dobla la fuerza de todas las cartas con habilidad `ESPIA` en campo (ambos jugadores). | Ambos campos |
| `LIDER_HALF_WEATHER` | Birna Bran | Las propias unidades no-héroe bajo clima activo contribuyen `ceil(fuerza/2)` en lugar de 1. | Solo el campo propio |

> Los líderes pasivos de doblado (DOUBLE_CAC, DOUBLE_RANGED, DOUBLE_SIEGE) se apilan con Commander's Horn cuando ambos están activos sin clima: `sumaNoHeroes × 2 (Horn) × 2 (líder) = × 4`.

### Interacciones entre líderes y otros efectos

| Situación | Resultado |
|---|---|
| LIDER_CANCEL_LEADER al crear partida | Ambos `liderUsado = true`; ningún jugador puede usar el endpoint de líder. |
| Líder pasivo + clima activo en la misma fila | El clima se aplica primero (no-héroe → 1 o `ceil(fuerza/2)` si HALF_WEATHER). El doblado del líder **no actúa** si hay clima. |
| LIDER_HALF_WEATHER + clima activo | No-héroe → `ceil(fuerza/2)` en lugar de 1. Los héroes son inmunes al clima (conservan fuerza base). |
| LIDER_DOUBLE_SPIES + héroes con ESPIA | Los héroes ESPIA **no** se doblan (inmunidad de héroes). |
| LIDER_SCORCH_CAC/RANGED + héroe en la fila | El héroe **sí** cuenta para el total ≥ 10, pero **no** puede ser destruido. |
| Turno del jugador con `jugadorPaso = true` | No se puede activar el líder (ya pasó la ronda). |

### DTO de request (`UsarLiderRequest`)

```json
{
  "targetCartaPartidaId": 123,    // Para LIDER_STEAL/RESTORE_GRAVEYARD
  "descartarIds": [45, 67]        // Para LIDER_DISCARD_DRAW (exactamente 2)
}
```

Los campos son opcionales para las habilidades que no los necesitan.

### Visibilidad del líder en `PartidaDTO`

El líder se expone en `JugadorPartidaDTO`:

```json
{
  "lider": { "id": 1, "nombre": "Foltest: Señor Comandante del Norte", "habilidad": "LIDER_CLEAR_WEATHER", ... },
  "liderUsado": false,
  "cartasReveladas": [...]         // Solo visible en `oponente` cuando Emhyr Emperador reveló cartas;
                                   // el propio jugador siempre recibe null en su propio objeto
}
```

---

## Orden de precedencia completo

El siguiente diagrama muestra la jerarquía de efectos al calcular la fuerza de una carta individual, y luego del total de la fila:

```
┌─────────────────────────────────────────────────────────┐
│          FUERZA DE UNA CARTA INDIVIDUAL                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  1. fuerza = fuerzaTransformada (si transformado=true)  │
│             o fuerza_base (catálogo)                    │
│                                                         │
│  2. ¿Es héroe?                                          │
│       SÍ → devuelve fuerza sin modificar  ◄─ PRIORIDAD │
│            (inmune a clima, Horn, MORAL, Scorch)        │
│       NO → continúa                                     │
│                                                         │
│  3. ¿Hay clima en esta fila?                            │
│       SÍ → devuelve 1                                   │
│       NO → continúa                                     │
│                                                         │
│  4. si ENLACE o VINCULO:                                │
│         fuerza = fuerza × copias_mismo_nombre_en_fila   │
│  5. fuerza += conteo_MORAL_en_fila (sin uno mismo)      │
│  → devuelve fuerza individual                           │
│                                                         │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│              TOTAL DE LA FILA                           │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  6. suma = Σ fuerzas individuales de todas las unidades │
│                                                         │
│  7. ¿Hay Commander's Horn en esta fila (slot o unidad)? │
│       SÍ → ¿Hay clima activo en esta fila?             │
│              SÍ  → total sin cambio (clima prevalece)   │
│              NO  → sumaHeroes + sumaNoHeroes × 2        │
│       NO → total sin cambio                             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

**Resumen de prioridades (de mayor a menor):**

| Prioridad | Efecto | Aplica a |
|---|---|---|
| 1 (máxima) | Inmunidad de héroe — fuerza sin modificar | Héroes (ignoran clima, Horn, MORAL, Scorch) |
| 2 | Clima → fuerza = 1 | No-héroes en fila(s) afectada(s) |
| 3 | Transformación Berserker | No-héroes con `transformado = true` |
| 4 | ENLACE / VINCULO (multiplicativo) | No-héroes con esa habilidad |
| 5 | REFUERZO_MORAL (aditivo) | No-héroes en la misma fila |
| 6 | Commander's Horn (×2 no-héroes) | Solo no-héroes de la fila (si no hay clima) |
| 7 (base) | Fuerza del catálogo | Todas las unidades |

---

## Tabla de interacciones

| Carta A | Carta B (misma fila) | Resultado |
|---|---|---|
| Unidad no-héroe | Clima activo en esa fila | Carta A → 1 |
| Héroe | Clima activo en esa fila | Héroe conserva fuerza normal |
| ENLACE (3 copias, fuerza 4, no-héroes) | — | Cada copia → 4×3 = 12 |
| ENLACE (2 copias no-héroe) + Clima | — | Cada copia → 1 |
| Héroe en fila con ENLACE de otros | — | Héroe conserva su fuerza; no multiplica ni es multiplicado por copias ajenas |
| Unidad no-héroe | MORAL en fila | +1 por cada carta MORAL |
| Unidad no-héroe | Clima + MORAL | → 1 (clima tiene prioridad; MORAL ignorado) |
| Héroe | MORAL en fila | Héroe NO recibe el +1 |
| Berserker (fuerza 6) | Mardroeme en fila | Berserker → transformado, fuerza 8 |
| Berserker (fuerza 6) | Sin Mardroeme | Berserker permanece en fuerza 6 |
| Berserker transformado (8) | Clima en fila | Si no es héroe → 1; si es héroe → 8 |
| No-héroes (total 15) | Commander's Horn | Total no-héroes → 30 |
| Héroe (15) + no-héroe (5) | Commander's Horn | Héroe = 15 (inmune); no-héroe 5×2=10; total = 25 |
| No-héroes (total 10) + Clima | Commander's Horn | No-héroes = 1 cada uno; total sin doblar |
| Héroe (8) + no-héroe (5) + Clima + Horn | — | Héroe = 8 (inmune a clima y Horn); no-héroe = 1 (clima gana, Horn anulado); total = 9 |
| ESPIA (jugado) | — | La carta va al campo del oponente |
| DECOY | Unidad en campo | Unidad vuelve a mano; DECOY toma su fila |
| MUSTER | Copias en mazo | Todas pasan a campo, misma fila |
| Buen clima | CLIMA en campo | Todos los CLIMA + Buen clima van al cementerio |
| SCORCH (total ≥ 10) | — | Destruye unidad(es) con mayor fuerza efectiva en ambos campos |
| SCORCH (total < 10) | — | Sin efecto |
| SCORCH_FILA (total enemigo en fila ≥ 10, héroes + Horn incluidos) | — | Destruye unidad(es) enemigas no-héroe más fuertes en esa fila |
| Cuerno del Comandante en slot | Mardroeme en mismo slot | Imposible: 422 — slot ocupado |
| Tormenta Skellige | Lluvia ácida | DISTANCIA afectada por ambas (efecto idéntico, no se apila) |

---

## Casos límite

### Clima + ENLACE (no-héroe)

El clima tiene prioridad absoluta. Una carta no-héroe con ENLACE_APRETADO en una fila con clima activo devuelve **1**, sin importar cuántas copias haya.

### Héroe con ENLACE/VINCULO

Un héroe que tuviera ENLACE_APRETADO o VINCULO_ESTRECHO **no multiplica su fuerza** — es inmune a ese efecto como a cualquier otro externo. Siempre devuelve su fuerza base.

### Clima + Horn (héroe vs no-héroe)

- No-héroe bajo clima + Horn: el no-héroe vale 1 (clima gana; Horn no lo dobla).
- Héroe bajo clima + Horn: el héroe vale su `fuerza` base sin modificar (inmune a clima **y** a Horn).

### MEDICO revive carta con MUSTER

La carta revivida llega al campo pero el efecto MUSTER **no se vuelve a disparar** (MUSTER solo se activa al jugar desde la mano). Las copias restantes en el mazo permanecen.

### MEDICO revive Berserker transformado

Al revivir un Berserker del cementerio, su estado `transformado` se resetea (`false`). Si al revivirlo hay Mardroeme en la fila, `procesarBerserker` no se ejecuta (MEDICO no llama a procesarAlJugar). El Berserker vuelve a su fuerza base.

> Para transformarlo después de revivir, se debería jugar Mardroeme de nuevo en esa fila.

### ESPIA en fila con clima

La carta ESPIA que va al campo del oponente sí es afectada por el clima activo en esa fila.

### SCORCH cuando ambos campos tienen la misma unidad más fuerte

Se destruyen **todas** las que tienen ese valor máximo, de cualquier jugador.

### SCORCH sobre Berserker transformado

La fuerza efectiva usada para Scorch es `fuerzaTransformada` (8), no `fuerza` base (6). El Berserker transformado es más vulnerable a Scorch.

### Scorch con héroes en campo

Los héroes **sí cuentan** para el cálculo del total (check ≥10), pero **no pueden ser destruidos** — ni por SCORCH global ni por SCORCH_FILA ni por LIDER_SCORCH_CAC/RANGED. Solo los no-héroes son elegibles para ser destruidos. Si todos los no-héroes del campo son más débiles que un héroe pero el total es ≥10, Scorch destruye al no-héroe más fuerte (no al héroe).

El bonus de **Commander's Horn** activo en la fila **también cuenta** para el total del check ≥10. Por ejemplo: 5 unidades de fuerza 1 + Horn = total 10 → Scorch activa.

### Mardroeme + Berserker: orden de juego

- Mardroeme primero → Berserker después: el Berserker se transforma al jugar (`procesarBerserker` detecta Mardroeme).
- Berserker primero → Mardroeme después: `procesarMardroeme` transforma todos los Berserkers ya en la fila.
- Ambos órdenes producen el mismo resultado.

### DECOY sobre carta con MUSTER

La carta retorna a mano normalmente. Al volver a jugarla, MUSTER **sí vuelve a dispararse**.

### DECOY sobre Berserker transformado

El Berserker regresa a la mano con `transformado = true` (el flag no se resetea). Al jugarlo de nuevo en una fila sin Mardroeme, **sigue transformado** (ya que el flag persiste en la entidad).

> Este es un caso inusual del engine — documentado como comportamiento esperado.

### Buen clima sin clima activo

No produce error. Buen clima simplemente va al cementerio sin mover nada más.

### Tormenta de Skellige + Buen clima

Buen clima elimina la Tormenta de Skellige como a cualquier otra carta CLIMA. Ambas filas (DISTANCIA y ASEDIO) recuperan la fuerza normal en el mismo acto.

### Fin de ronda con slot lateral ocupado

Las cartas en slot lateral van al cementerio junto con las demás cartas de campo. El slot queda libre para la siguiente ronda.

### Múltiples MORAL en la misma fila

Si hay N cartas MORAL en la fila, cada unidad no-héroe recibe `+N` excepto las propias cartas MORAL, que reciben `+(N-1)`. Los héroes no reciben ningún bonus.