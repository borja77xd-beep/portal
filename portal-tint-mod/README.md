# Portal Tint

Mod de Fabric para Minecraft 1.21.1 que permite tintar (colorear) los portales
del Nether con los tintes (dyes) del juego. El cambio es **puramente visual**:
no modifica el comportamiento del portal (teletransporte, generación, daño,
sonidos, etc.). Solo jugadores con **permiso de OP (nivel 2)** pueden aplicar
el tinte.

## Cómo funciona

1. Clic derecho sobre cualquier bloque de un portal activo con un tinte en la mano.
2. El mod busca (BFS) todos los bloques `minecraft:nether_portal` conectados
   a ese bloque y les asigna el color del tinte usado.
3. El color se guarda en un `PersistentState` por mundo y se sincroniza a
   todos los clientes conectados.
4. El renderizado tintado se logra mediante:
   - Un override del modelo vanilla `nether_portal.json` que añade `tintindex`
     a las caras (sin tocar la lógica del bloque).
   - Un `BlockColorProvider` registrado solo en el cliente que consulta el
     color recibido por red para cada posición.

No se usan mixins que alteren el comportamiento del juego.

## Requisitos para compilar

- JDK 21
- Conexión a internet (para descargar Minecraft, Yarn mappings y Fabric API
  la primera vez)

## Compilar localmente

```bash
./gradlew build
```

El `.jar` resultante queda en `build/libs/`.

> **Nota:** este repositorio incluye `gradle/wrapper/gradle-wrapper.properties`
> pero **no** el binario `gradle-wrapper.jar` (no se puede generar sin acceso
> a red). Antes del primer build local, genera el wrapper con una instalación
> de Gradle local:
> ```bash
> gradle wrapper --gradle-version 8.8
> ```
> Esto crea `gradlew`, `gradlew.bat` y `gradle/wrapper/gradle-wrapper.jar`.
> El workflow de GitHub Actions incluido (`.github/workflows/build.yml`) hace
> esto automáticamente en CI una vez que el wrapper esté commiteado, o puedes
> ajustar el workflow para usar `gradle` directamente si prefieres no
> commitear el wrapper.

## CI/CD (GitHub Actions)

El workflow en `.github/workflows/build.yml`:

- Se ejecuta en cada `push`/`pull_request` a `main` y en tags `vX.Y.Z`.
- Compila el mod con JDK 21.
- Sube el `.jar` como artifact descargable de cada ejecución.
- Si el push es un tag `vX.Y.Z`, crea automáticamente un GitHub Release con
  el `.jar` adjunto.

## Estructura del proyecto

```
src/main/java/com/example/portaltint/         -> lógica común (servidor + cliente)
src/main/java/com/example/portaltint/network/ -> paquetes de sincronización
src/client/java/com/example/portaltint/client/ -> registro del color y cache visual
src/main/resources/fabric.mod.json            -> metadata del mod
src/main/resources/assets/minecraft/...       -> override visual del modelo del portal
```

## Estado / TODO

- [ ] Verificar los nombres exactos de la API (`NbtCompound`, `PacketCodecs`,
      etc.) contra las Yarn mappings `1.21.1+build.3` reales al compilar
      (pueden variar ligeramente entre builds de mappings).
- [ ] Confirmar `fabric_version` y `loader_version` vigentes en
      https://fabricmc.net/develop/
- [ ] Añadir icono del mod (`assets/portaltint/icon.png`, 128x128).
- [ ] Opcional: comando `/portaltint clear` para limpiar el tinte de un portal.
