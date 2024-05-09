# Hundir la Flota

## Descripción
"Hundir la Flota" es un juego de tablero clásico para dos jugadores: un usuario y el ordenador. Cada jugador tiene un tablero de 10x10 posiciones (A-J, 0-9) donde se colocan los barcos en orientación horizontal o vertical. El objetivo es hundir los barcos del oponente adivinando sus coordenadas.

## Reglas del Juego
- Cada jugador coloca 2 barcos de 5 casillas, 3 de 3 casillas y 5 de 1 casilla.
- Los jugadores se turnan para lanzar bombas diciendo coordenadas.
- El oponente indica si la bomba toca un barco o cae en el agua.
- No se detecta si un tiro hunde completamente un barco.
- Cada jugador comienza con 24 puntos, que se van restando con cada acierto del oponente.
- Se lleva un registro de los disparos para evitar repetir coordenadas.

## Características Técnicas
- El juego ha sido actualizado para funcionar sobre una arquitectura cliente-servidor mediante TCP.
- El servidor procesa el resultado del disparo y envía las coordenadas correspondientes.
- Se implementa un sistema de ranking que guarda el nombre y el número de disparos necesarios para derrotar al servidor.

## Menú Inicial
1. Jugar
2. Records
3. Salir