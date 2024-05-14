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

## Instalación

Para instalar y ejecutar el juego, sigue estos pasos:

1. Clona el repositorio: `git clone https://github.com/byronnDev/HundirLaFlota.git`
2. Navega al directorio del proyecto: `cd HundirLaFlota\src\hundirflota`
3. Compila y ejecuta el programa: `java hundirflota.Main.java`

## Uso

Al iniciar el juego, se te presentará un menú con las siguientes opciones:

1. Jugar: Comienza un nuevo juego.
2. Records: Muestra el ranking de jugadores.
3. Salir: Cierra el juego.

Durante el juego, se te pedirá que introduzcas las coordenadas (A-J, 0-9) donde quieres lanzar una bomba. El juego continuará hasta que todos los barcos de un jugador hayan sido hundidos.

## Contribución

Si deseas contribuir al proyecto, por favor, abre un issue o realiza un pull request.

## Licencia

Este proyecto está bajo la licencia MIT. Para más información, consulta el archivo LICENSE.