package com.arimar.gwent.ingame_service.controller;

import com.arimar.gwent.ingame_service.dto.JugadorRequestDto;
import com.arimar.gwent.ingame_service.exceptions.DatoDeJugadorInvalidoException;
import com.arimar.gwent.ingame_service.exceptions.NoSePuedeBorrarJugadorConCartasException;
import com.arimar.gwent.ingame_service.model.Jugador;

import com.arimar.gwent.ingame_service.service.JugadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/jugador")
public class JugadorController {

    private JugadorService jugadorService;


    public JugadorController(JugadorService jugadorService){
        this.jugadorService = jugadorService;

    }
    @GetMapping
    public List<Jugador> traerTodosLosJugadores() {
        return jugadorService.traerTodosLosJugadores();

    }

    @GetMapping("/{id}")
    public Jugador traerUnJugadorPorId(@PathVariable int id) {
        Optional<Jugador> jugadorOptional = jugadorService.traerJugadorPorId(id);
        if (jugadorOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro :(");
        }
        return jugadorOptional.get();
    }

    @DeleteMapping("/{id}")
    public Jugador eliminarJugador(@PathVariable("id") int id) {
        try {
            Optional<Jugador> jugadorOptional = jugadorService.traerJugadorPorId(id);
            if (jugadorOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontro :(");
            }
            Jugador jugadorEliminar = jugadorOptional.get();
            jugadorService.eliminarJugador(jugadorEliminar);
            return jugadorEliminar;
        } catch (NoSePuedeBorrarJugadorConCartasException e) {
            System.err.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping
    public Jugador guardarJugador(@RequestBody JugadorRequestDto jugadorRequestDto) throws DatoDeJugadorInvalidoException {
        if (jugadorRequestDto.getApodo().isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try{
            return jugadorService.guardarJugador(jugadorRequestDto);
        }catch (Exception e){
            System.err.println(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Jugador> guardarJugadorModificado(@RequestBody JugadorRequestDto jugadorRequestDto, @PathVariable("id") int id) {
        Optional<Jugador> jugadorOptional = jugadorService.traerJugadorPorId(id);
        if (jugadorOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            jugadorService.guardarJugador(jugadorRequestDto);
            return ResponseEntity.ok(jugadorOptional.get());
        }catch (Exception e){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

    }
}
