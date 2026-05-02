package com.arimar.gwent.ingameservice.service;

import com.arimar.gwent.ingameservice.domain.enums.FilaCarta;
import com.arimar.gwent.ingameservice.domain.enums.ZonaCarta;
import com.arimar.gwent.ingameservice.entity.CartaPartida;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Map;

/**
 * Centralizes all ZonaCarta state transitions for CartaPartida instances.
 *
 * Allowed transitions:
 *   MAZO        → MANO        (deal / draw between rounds / mulligan draw)
 *   MANO        → MAZO        (mulligan return)
 *   MANO        → CAMPO       (play card — requires FilaCarta)
 *   MANO        → CEMENTERIO  (instant-use specials like Buen clima that clear after triggering)
 *   CAMPO       → CEMENTERIO  (round resolution — clears filaEnCampo)
 *   CAMPO       → MANO        (DECOY ability — target unit returns to hand)
 *   CEMENTERIO  → CAMPO       (MEDICO ability — revive non-hero unit to field)
 *   CEMENTERIO  → MANO        (leader: steal/restore from graveyard to hand)
 *   CEMENTERIO  → MAZO        (leader: Crach an Craite shuffles graveyards back)
 */
@Slf4j
@Component
public class CartaPartidaStateMachine {

    private static final Map<ZonaCarta, EnumSet<ZonaCarta>> TRANSICIONES = Map.of(
            ZonaCarta.MAZO,       EnumSet.of(ZonaCarta.MANO),
            ZonaCarta.MANO,       EnumSet.of(ZonaCarta.MAZO, ZonaCarta.CAMPO, ZonaCarta.CEMENTERIO),
            ZonaCarta.CAMPO,      EnumSet.of(ZonaCarta.CEMENTERIO, ZonaCarta.MANO),
            ZonaCarta.CEMENTERIO, EnumSet.of(ZonaCarta.CAMPO, ZonaCarta.MANO, ZonaCarta.MAZO)
    );

    /**
     * MAZO → MANO. Used when dealing initial cards and drawing between rounds.
     */
    public void robar(CartaPartida carta) {
        transicionar(carta, ZonaCarta.MANO);
    }

    /**
     * MANO → MAZO. Used when a player returns a card during mulligan.
     */
    public void devolverAlMazo(CartaPartida carta) {
        transicionar(carta, ZonaCarta.MAZO);
    }

    /**
     * MANO → CAMPO. Used when playing a unit card.
     * Sets filaEnCampo to the resolved row.
     */
    public void jugar(CartaPartida carta, FilaCarta fila) {
        transicionar(carta, ZonaCarta.CAMPO);
        carta.setFilaEnCampo(fila);
    }

    /**
     * CAMPO → CEMENTERIO. Used during round resolution.
     * Clears filaEnCampo.
     */
    public void descartar(CartaPartida carta) {
        transicionar(carta, ZonaCarta.CEMENTERIO);
        carta.setFilaEnCampo(null);
    }

    /**
     * CEMENTERIO → CAMPO. MEDICO ability: revives a non-hero unit from the graveyard.
     * Sets filaEnCampo.
     */
    public void revivirCarta(CartaPartida carta, FilaCarta fila) {
        transicionar(carta, ZonaCarta.CAMPO);
        carta.setFilaEnCampo(fila);
    }

    /**
     * CAMPO → MANO. DECOY ability: the targeted unit returns to the player's hand.
     * Clears filaEnCampo.
     */
    public void retornarAMano(CartaPartida carta) {
        transicionar(carta, ZonaCarta.MANO);
        carta.setFilaEnCampo(null);
    }

    /**
     * MANO → CEMENTERIO. For instant-use cards that trigger and leave no field presence.
     * Also used by Eredin Rompenieblas leader: discard cards from hand.
     */
    public void usarInstantaneo(CartaPartida carta) {
        transicionar(carta, ZonaCarta.CEMENTERIO);
    }

    /**
     * CEMENTERIO → MANO. Leader ability: take a card from own or opponent's graveyard to hand.
     */
    public void tomarDelCementerio(CartaPartida carta) {
        transicionar(carta, ZonaCarta.MANO);
    }

    /**
     * CEMENTERIO → MAZO. Leader ability: Crach an Craite shuffles graveyards back to decks.
     */
    public void devolverCementerioAlMazo(CartaPartida carta) {
        transicionar(carta, ZonaCarta.MAZO);
    }

    // ------------------------------------------------------------------

    private void transicionar(CartaPartida carta, ZonaCarta destino) {
        ZonaCarta origen = carta.getZona();
        EnumSet<ZonaCarta> permitidos = TRANSICIONES.getOrDefault(origen, EnumSet.noneOf(ZonaCarta.class));
        if (!permitidos.contains(destino)) {
            throw new IllegalStateException(
                    "Transición de carta inválida: %s → %s (cartaPartidaId=%d)"
                            .formatted(origen, destino, carta.getId())
            );
        }
        carta.setZona(destino);
        log.debug("[carta-{}] '{}': {} → {}", carta.getId(),
                carta.getCartaCatalogo().getNombre(), origen, destino);
    }
}
