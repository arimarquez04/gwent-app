package com.arimar.gwent.ingameservice.repository;

import com.arimar.gwent.ingameservice.domain.Faccion;
import com.arimar.gwent.ingameservice.domain.FilaCarta;
import com.arimar.gwent.ingameservice.domain.HabilidadCarta;
import com.arimar.gwent.ingameservice.domain.TipoCarta;
import com.arimar.gwent.ingameservice.entity.CartaCatalogo;
import org.springframework.data.jpa.domain.Specification;

public class CartaCatalogoSpec {

    private CartaCatalogoSpec() {}

    public static Specification<CartaCatalogo> withFaccion(Faccion faccion) {
        return (root, query, cb) ->
                faccion == null ? null : cb.equal(root.get("faccion"), faccion);
    }

    public static Specification<CartaCatalogo> withFila(FilaCarta fila) {
        return (root, query, cb) ->
                fila == null ? null : cb.equal(root.get("fila"), fila);
    }

    public static Specification<CartaCatalogo> withTipo(TipoCarta tipo) {
        return (root, query, cb) ->
                tipo == null ? null : cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<CartaCatalogo> withEsHeroe(Boolean esHeroe) {
        return (root, query, cb) ->
                esHeroe == null ? null : cb.equal(root.get("esHeroe"), esHeroe);
    }

    public static Specification<CartaCatalogo> withHabilidad(HabilidadCarta habilidad) {
        return (root, query, cb) ->
                habilidad == null ? null : cb.equal(root.get("habilidad"), habilidad);
    }

    /** Retorna cartas de tipo CLIMA o ESPECIAL (cartas no-unidad). */
    public static Specification<CartaCatalogo> esEspecialOClima() {
        return (root, query, cb) ->
                root.get("tipo").in(TipoCarta.CLIMA, TipoCarta.ESPECIAL);
    }
}
