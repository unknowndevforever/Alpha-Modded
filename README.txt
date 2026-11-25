recreatecraft patch - source
============================
The game jar is the original obfuscated Alpha 1.1.2 (classes like ly, oc, fr).
This patch = new classes + a bytecode patcher (JDK-internal ASM, needs JDK 21).

src/PoweredRail.java   powered rail block (id 27 off, id 28 on), extends ly (Block)
src/Ext.java           helpers: FOV, creative mode, flight, cart boost, coords, recipe
src/CreativeScreen.java creative inventory screen (extends bh), chest-texture look,
                       edits the real inventory (eu.a[0..35]) so it saves with the world
src/Names.java         item names for the creative tooltips
src/WorldsScreen.java  replaces Select world (jq): create / delete / edit world
src/WorldSettingsScreen.java  Create New World / Edit World (name, game mode, allow cheats)
src/Inv.java           shift click / right click drag in every container screen (ee)
src/HttpSocket.java    Socket that tunnels the game protocol over HTTP (for the Next.js server)
src/SkinScreen.java    Change Skin screen + skin URLs (.minecraft/skin.png, server skins)
src/NetFx.java         handlers for the recreatecraft server -> client packets (hurt, sound, weather)
src/WeatherFx.java     rain: sky/fog/sky light darkening, rain streaks, splashes, rain sound
src/PacketHurt.java    packet 0x08, src/PacketSound.java 0x3e, src/PacketWeather.java 0x46 (extend fn)
src/Patch.java         ASM patcher: edits the original classes (ly, dw, oc, jo, mk,
                       fr, iq, ay, lu, bi, ge, dm, cx, Minecraft, ee, nt, kz, gy, gc)
server/                Next.js multiplayer server (wasmer.app), see server/README.md
stubs/                 LWJGL stubs, ONLY for compiling (do not put in the jar)
textures/              16x16 powered rail tiles (terrain.png tiles 129 = off, 130 = on)
scripts/               the helper scripts I used (paths are from my sandbox; edit them)

Build on Windows (one step)
---------------------------
  powershell -ExecutionPolicy Bypass -File scripts\build.ps1
Uses the a1.1.2_01 jar, ASM 9.8 and LWJGL 2.9.0 from the Modrinth app folder (override
with -Jar / -Asm / -Lwjgl). Output: dist\recreatecraft_patched.jar (temp files in work\).

Build steps (manual, old)
-------------------------
1. Unzip the original jar to a folder, e.g. jar/
2. Compile the new classes (Java 8 target, jar + stubs on the classpath):
     javac -source 8 -target 8 -cp jar:stubs -d build src/PoweredRail.java src/Ext.java src/CreativeScreen.java
   then delete build/org (the compiled stubs).
3. Run the patcher on the unzipped jar folder:
     javac --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -d pbuild src/Patch.java
     java  --add-exports java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED -cp pbuild Patch jar out
4. Copy out/ into a package folder, add build/*.class, put the two textures into
   terrain.png at tile 129 and 130 (16x16 tiles, row = tile/16, col = tile%16),
   delete META-INF/*.SF and *.DSA, and zip it as the new jar.

Fancy/Fast graphics crash: the renderer's chunk sorts (class e, dw) now go through Ext.sort,
a merge sort that cannot throw "Comparison method violates its general contract".
F3 shows the vanilla "N fps, M chunk updates" title line plus an "FPS: N" line under the coords.

World settings live in saves/WorldN/recreatecraft.txt (name, cheats, creative).
Controls: F4 creative on/off (only if the world has cheats on), E item menu, double-tap
jump = fly, F3 shows coordinates, FOV slider in Options.
Creative menu: left click = stack, right click = 1, shift click = into inventory,
click the item grid while holding something = delete it, 1-9 over an item = put it in
that hotbar slot, shift click an inventory slot = clear it.
Recipe: 1 rail above 1 redstone = 1 powered rail.

Inventory / crafting / chest screens:
  shift click the crafting output = craft as many as fit into your inventory
  shift click any other slot      = move the stack (hotbar <-> inventory, chest <-> inventory,
                                    crafting grid / armor -> inventory)
  hold right click and drag       = drop one item into every slot you pass over

Change Skin (title screen, where "Play tutorial level" was): pick a 64x32 or 64x64 PNG, it is
saved as .minecraft/skin.png (64x64 skins are cut to the top half, this version only knows 64x32).

Multiplayer: type name.wasmer.app (or localhost:3000) to join a server/ Next.js server through
HTTP. Normal host:port addresses still connect over TCP like before. Address limit is 64 chars.

Server -> client packets (recreatecraft servers only, ids unused by vanilla; big endian, strings = writeUTF)
--------------------------------------------------------------------------------------------------------
Registered at the end of fn.<clinit> by Patch.java. Handlers run on the game thread (ii.a() drains the
packet queue from the game tick), every handler is wrapped in try/catch.
  0x08 HURT     short damage (1..20; clamped). Calls the player's own attackEntityFrom (dm.a(null, dmg)):
                armor absorption + armor wear, hurt flash/tilt, random.hurt sound, invulnerability frames,
                death -> the normal game over screen, Respawn works as before. Ignored in creative mode
                and while dead. NOTE: like vanilla, a player ignores ALL damage during the first half of
                the invulnerability window (10 ticks = 0.5 s after a hit), so send HURTs >= 0.5 s apart.
                Dying drops the inventory through the vanilla multiplayer path (pickup spawn packets).
  0x3e SOUND    utf name, int x, int y, int z (block coords * 8), float volume, float pitch.
                Plays World.playSound(x/8, y/8, z/8, name, volume, pitch): heard within 16 blocks
                (16 * volume when volume > 1). Unknown names play nothing. Pitch clamped to 0.5..2.
  0x46 WEATHER  byte state: 0 = clear, 1 = rain. Rain fades in/out over ~6 s: greyer/darker sky and fog,
                sky light dimmed by up to 3 levels (like Beta), rain streaks around the player (only where
                the column is open to the sky), splash particles, ambient.weather.rain sound. Reset to
                clear on every world change (joining, disconnecting, title screen).

Sound names (= sound file path under resources/sound or resources/newsound, "/" -> ".", trailing digits
dropped; a name with several numbered files picks one at random). What this client itself plays:
  random.hurt random.explode random.fuse random.click random.pop random.fizz random.bow random.drr
  random.door_open random.door_close random.glass random.splash
  step.stone step.wood step.gravel step.grass step.cloth step.sand  (block step sounds)
  liquid.water fire.fire fire.ignite ambient.cave.cave
  mob.chicken mob.chickenhurt mob.chickenplop mob.cow mob.cowhurt mob.pig mob.pigdeath mob.sheep
  mob.creeper mob.creeperdeath mob.skeleton mob.skeletonhurt mob.spider mob.spiderdeath mob.slime
  mob.slimeattack mob.zombie mob.zombiehurt mob.zombiedeath
The game loads every file the launcher puts in resources/, so with the usual legacy asset index
(pre-1.6.json) these also work: ambient.weather.rain ambient.weather.thunder damage.hurtflesh
damage.fallbig damage.fallsmall liquid.lava liquid.lavapop liquid.splash random.break random.breath
random.burp random.chestopen random.chestclosed random.drink random.eat random.levelup random.orb
random.bowhit random.old_explode step.snow note.bass note.bassattack note.bd note.harp note.hat note.pling
note.snare portal.portal portal.travel portal.trigger tile.piston.in tile.piston.out mob.wolf.* mob.cat.*
mob.ghast.* mob.blaze.* mob.endermen.* mob.silverfish.* mob.magmacube.* mob.zombiepig.* mob.irongolem.*
