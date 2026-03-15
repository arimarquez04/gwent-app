package com.arimar.gwent.ingame_service.dto;
import com.arimar.gwent.ingame_service.model.Jugador;

@Deprecated
public class JugadorRequestDto {
    private String apodo;

    public String getApodo() {
        return apodo;
    }

    public void setApodo(String apodo) {
        this.apodo = apodo;
    }

    public Jugador convert(){
        Jugador jugador = new Jugador();
        jugador.setApodo(apodo);
        return jugador;
    }

}
