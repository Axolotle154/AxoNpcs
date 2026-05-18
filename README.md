# AxoNPCs

AxoNPCs is a Paper/Folia 26.1.2+ plugin for client-side NPCs. NPCs are sent to players with packets and are not spawned as real server entities.

## Required Server Plugins

Install PacketEvents 2.12.1+ on the server. AxoNPCs uses PacketEvents as an external dependency to keep the AxoNPCs jar small and avoid bundling Netty twice.

## Build

```bash
mvn package
```

The plugin jar is generated at:

```text
target/AxoNPCs-1.0.4-alfa.jar
```

## Runtime Files

On first start the plugin creates:

```text
AxoNPCs/
├─ config.yml
├─ version.yml
├─ languages/
│  ├─ en.yml
│  ├─ es.yml
│  └─ ru.yml
├─ logs/
├─ skins/
└─ npcs/
   ├─ npc_1.yml
   ├─ npc_2.yml
   └─ ejemplo.yml
```

Every NPC is stored in its own YAML file under `npcs/`. File names are based on validated NPC IDs and only allow letters, numbers, `_`, and `-`.

## API Example

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

Public API classes are under `org.axostudio.axonpcs.api`; internal implementation classes are kept outside that package.
