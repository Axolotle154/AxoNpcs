# AxoNPCs

AxoNPCs es un plugin para Paper/Folia que crea NPCs client-side usando paquetes nativos de Paper. Los NPCs no se generan como entidades reales del servidor: cada jugador los ve, oculta e interactua con ellos desde su propio cliente.

El plugin esta pensado para crear guias, accesos rapidos, decoracion interactiva o puntos de informacion sin cargar el mundo con entidades persistentes. Soporta nombres con MiniMessage/colores legacy, skins de jugadores, brillo, escala, colisiones, cooldowns de interaccion y acciones por click.

## Compatibilidad

- Servidor: Paper/Folia 26.1.2 o superior.
- Java: 25 o superior.

## Archivos del plugin

```text
AxoNPCs/
├─ config.yml
├─ languages/
│  ├─ en.yml
│  ├─ es.yml
│  └─ ru.yml
├─ logs/
├─ skins/
└─ npcs/
   └─ ejemplo.yml
```

Cada NPC se guarda en su propio archivo YAML dentro de `npcs/`. El nombre del archivo se basa en el ID del NPC y solo permite letras, numeros, `_` y `-`.

## Comandos

Comandos principales:

| Comando | Descripcion |
| --- | --- |
| `/axonpcs version` | Muestra la version del plugin y del servidor. |
| `/axonpcs reload` | Recarga configuracion, mensajes y NPCs guardados. Tambien refresca los NPCs visibles. |
| `/axonpcs featureflags` | Muestra los feature flags activos desde `config.yml`. |
| `/axonpc ...` | Alias de `/axonpcs`. |

Comandos de NPC:

| Comando | Descripcion |
| --- | --- |
| `/npc` o `/npc help` | Muestra la ayuda del comando. |
| `/npc create <id> [--position <x y z>] [--world <world>] [--type <type>]` | Crea un NPC. Si lo ejecuta un jugador sin opciones, usa su posicion actual. |
| `/npc remove <id>` | Elimina el NPC y su archivo. |
| `/npc list [--type <type>] [--sort id\|type\|world]` | Lista los NPCs guardados, con filtro y orden opcional. |
| `/npc nearby [radius]` | Lista NPCs cercanos al jugador. Solo jugadores. Por defecto usa radio `16`. |
| `/npc info <id>` | Muestra datos basicos del NPC: tipo, mundo, posicion y viewers. |
| `/npc type <id> <type>` | Cambia el tipo guardado del NPC. Actualmente el backend nativo implementa `PLAYER`. |
| `/npc displayname <id> [name\|@none]` | Consulta o cambia el nombre visible. Usa `@none` para quitarlo. |
| `/npc skin <id> <skin\|@none\|@mirror>` | Cambia la skin. Puede usar un nombre de jugador, quitarla o espejar la del viewer. |
| `/npc glowing <id> <color\|off>` | Activa o desactiva el brillo del NPC con color. |
| `/npc collidable <id> <true\|false>` | Define si el NPC debe ser colisionable desde el cliente. |
| `/npc scale <id> <factor>` | Cambia la escala visual del NPC. |
| `/npc turn_to_player <id> [true\|false]` | Define si el NPC debe mirar al jugador cuando esta cerca. Sin estado muestra el valor actual. |
| `/npc turn_to_player_distance <id> [distance]` | Cambia la distancia a la que el NPC empieza a mirar al jugador. Sin distancia muestra el valor actual. |
| `/npc movehere <id>` | Mueve el NPC a la posicion del jugador. Solo jugadores. |
| `/npc moveto <id> <x> <y> <z> [world]` | Mueve el NPC a coordenadas concretas. |
| `/npc center <id>` | Centra el NPC en el bloque donde esta, conservando altura y rotacion. |
| `/npc rotate <id> <yaw> <pitch>` | Cambia la rotacion del NPC. |
| `/npc teleport <id>` | Teletransporta al jugador al NPC. Solo jugadores. |
| `/npc action <id> <trigger> add <type> <value>` | Agrega una accion al NPC. |
| `/npc action <id> <trigger> list` | Lista las acciones de un trigger. |
| `/npc action <id> <trigger> remove <index>` | Elimina una accion por indice. |
| `/npc interactioncooldown <id> <disabled\|seconds>` | Cambia el cooldown de interaccion del NPC. |

Triggers de acciones: `RIGHT_CLICK`, `LEFT_CLICK`, `ANY`.

Colores de glowing sugeridos: `white`, `green`, `aqua`, `red`, `yellow`, `blue`, `gold`, `gray`, `dark_gray`, `black`, `dark_blue`, `dark_green`, `dark_aqua`, `dark_red`, `dark_purple`, `light_purple`, `pink` y `off`.

Tipos de accion incluidos:

| Tipo | Resultado |
| --- | --- |
| `MESSAGE` | Envia un mensaje al jugador. |
| `PLAYER_COMMAND` o `COMMAND` | Ejecuta un comando como el jugador. |
| `CONSOLE_COMMAND` o `SERVER` | Ejecuta un comando desde consola. |

En los valores de acciones se pueden usar `{player}`, `{npc}` y `{world}`. PlaceholderAPI tambien se aplica si esta activo en `config.yml`.

## Permisos

`/npc help` y `/npc` sin argumentos muestran ayuda sin permiso especial. El resto de comandos valida permisos.

| Permiso | Default | Permite |
| --- | --- | --- |
| `axonpcs.admin` | `op` | Acceso completo a AxoNPCs. |
| `axonpcs.command.*` | `op` | Acceso a todos los comandos del plugin. |
| `axonpcs.command.version` | `true` | Usar `/axonpcs version`. |
| `axonpcs.command.reload` | `op` | Usar `/axonpcs reload`. |
| `axonpcs.command.featureflags` | `op` | Usar `/axonpcs featureflags`. |
| `axonpcs.command.npc.*` | `op` | Acceso a todos los subcomandos de `/npc`. |
| `axonpcs.command.npc.create` | `op` | Crear NPCs con `/npc create`. |
| `axonpcs.command.npc.remove` | `op` | Eliminar NPCs con `/npc remove`. |
| `axonpcs.command.npc.list` | `op` | Listar NPCs con `/npc list` y `/npc nearby`. |
| `axonpcs.command.npc.info` | `op` | Ver informacion con `/npc info`. |
| `axonpcs.command.npc.type` | `op` | Cambiar tipo con `/npc type`. |
| `axonpcs.command.npc.displayname` | `op` | Cambiar nombre visible con `/npc displayname`. |
| `axonpcs.command.npc.skin` | `op` | Cambiar skin con `/npc skin`. |
| `axonpcs.command.npc.glowing` | `op` | Cambiar `glowing`, `collidable`, `scale` e `interactioncooldown`. |
| `axonpcs.command.npc.turn_to_player` | `op` | Usar `/npc turn_to_player`. |
| `axonpcs.command.npc.turn_to_player_distance` | `op` | Usar `/npc turn_to_player_distance`. |
| `axonpcs.command.npc.move` | `op` | Usar `/npc movehere`, `/npc moveto`, `/npc center` y `/npc rotate`. |
| `axonpcs.command.npc.teleport` | `op` | Usar `/npc teleport`. |
| `axonpcs.command.npc.action` | `op` | Crear, listar y eliminar acciones de NPCs. |

## API

Las clases publicas estan bajo `org.axostudio.axonpcs.api`. Las clases fuera de ese paquete son internas.

```java
import org.axostudio.axonpcs.api.AxoNPCsAPI;
import org.axostudio.axonpcs.api.AxoNPCsProvider;

AxoNPCsAPI api = AxoNPCsProvider.getAPI();

api.createNPC("guia", location);
api.deleteNPC("guia");
api.getNPC("guia");
api.showNPC(player, "guia");
api.hideNPC(player, "guia");
api.exists("guia");
api.getNPCs();
```
