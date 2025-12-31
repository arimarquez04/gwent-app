# Ms-Gwent
Backend para una aplicación de simulación del juego Gwent, basado en microservicios con Spring Boot. Incluye servicios para gestionar jugadores, solicitudes de partidas, lógica de juego, rankings y más, todo con un enfoque escalable y modular.

1. jugador-service
   Responsabilidades:
   -Registro de jugadores: Creación de cuentas con usuario, contraseña, apodo y un número único de 4 dígitos.
   -Autenticación y autorización: Maneja el login con usuario y contraseña.
   -Gestión de perfiles: Permite ver y actualizar el perfil del jugador, incluyendo estadísticas y apodo.

2. desafio-service
   Responsabilidades:
   -Envío de solicitudes de partida: Permite a un jugador enviar una solicitud de partida a otro.
   -Gestión de aceptación o rechazo: El receptor puede aceptar o rechazar la solicitud.
   -Notificaciones: Notifica al ingame-service para iniciar la partida si se acepta o simplemente descarta la solicitud si se rechaza.

4. ingame-service (Reemplaza a mazo-service, carta-service, y partida-service)
   Responsabilidades:
   -Gestión de mazos: Creación, edición y eliminación de mazos de cartas, y asignación de cartas a los mazos de los jugadores.
   -Gestión de cartas: Almacena y proporciona información sobre las cartas del juego (habilidades, facción, fuerza, etc.).
   -Lógica de la partida: Coordina la creación de partidas, manejo de turnos, jugadas y determina los resultados de las partidas.

5. ranking-service
   Responsabilidades:
   -Seguimiento de partidas: Registra y mantiene el historial de partidas jugadas.
   -Cálculo de clasificación: Genera rankings basados en los resultados de las partidas y las estadísticas de los jugadores.

5. bff-service (Backend for Frontend)
   Responsabilidades:
   -Orquestación de llamadas a microservicios: Simplifica la interacción entre el frontend y los microservicios, agregando datos de diferentes servicios.
   -Gestión de autenticación: Maneja la autenticación centralizada del usuario y las sesiones de login.
   -Punto de entrada para el cliente: Proporciona una API simplificada para que el frontend se conecte a los microservicios de manera eficiente.