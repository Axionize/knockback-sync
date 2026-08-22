## Project Structure

- `common` submodule contains shared code for all platforms.
  - It contains the interfaces and abstractions for
    - Permissions
    - Scheduling
    - Players
    - Blocks
    - Worlds/Levels
    - The Server
  - In addition to the interfaces, the code also contains the implementations for
    - Commands
      - Commands use incendio's `cloud` command system. All you have to do to add platform support is to create a CommandManager and call register(). 
    - Handling Events
    - Gathering Statistics
- `fabric` contains code for the latest supported Fabric version (currently Minecraft 26.2)
- `bukkit` contains bukkit-specific code

If you want to add support for a new platform, simply make a new submodule and implement the interfaces for.
- Scheduling Tasks
- Checking for Permissions
- Listening for events and calling the right handler
- Players + Blocks + World + Server
- Metrics

You will need to handle events when Players:
- join/leave
- are damaged
- take velocity (knock back)

If you find your platform is lacking events, take a look at how the PlayerVelocityEvent is implemented fabric for some direction of what to do


## List of version and platform-specific caveats

- The optional Grim integration uses the same published Grim API on Bukkit and Fabric. KBS still bundles the Grim
  PacketEvents fork on Fabric and falls back to PacketEvents for users Grim does not track.

## Grim latency integration verification

`GrimLatencyIntegrationHarnessTest` uses the published Grim API's real typed event-channel classes. It fires a
`GrimTransactionReceivedEvent` through the production Grim provider and verifies the complete API event -> KBS
sample path, initial platform-ping fallback, join/quit/disable behavior, and the synthetic-send gate. The smaller
service/controller tests cover provider replacement, fallback, jitter/previous-ping state, fail-closed behavior,
and the optional Fabric classloading boundary.

The implementation was also booted on a real Minecraft 26.2 Fabric server with Fabric Loader 0.19.3, a real public
Grim 2.0 Fabric build, and two real HeadlessMC 26.2 clients. KBS selected Grim without linkage errors. For a player
tracked by Grim, `/knockbacksync ping` first reported Grim's 40 ms transaction ping and later 6 ms with 11.679 ms
jitter. The server log contained no `BadPacketsO`, `31407`, or `31408` entry. This proves real Fabric/Grim API
linkage and live sample propagation. The absence of IDs in the ordinary server log is not a packet-stream proof;
the production send-gate invariant is proved by the API-faithful harness. A scripted HeadlessMC combat attempt was
not counted as evidence because the 26.2 key injector recursed while simulating the attack key.

KBS is pinned to the PacketEvents snapshot used by the current public Grim 2.0 source
(`2.13.1+4d40422-SNAPSHOT`), not upstream PacketEvents. The live profile's installed Grim build supplied the newer
Grim-fork snapshot `2.13.1+8187e23-SNAPSHOT`; Fabric Loader correctly selected that single runtime copy.

Run this Hoplite Bukkit dev-server smoke test before deployment:

1. Build with `./gradlew clean build`, stop Hoplite, remove the old KnockbackSync jar, and copy
   `bukkit/build/libs/knockbacksync-bukkit-*.jar` into `plugins/`. Remove the old per-server Grim packet exception.
2. Leave `latency.provider: "AUTO"` in `plugins/KnockbackSync/config.yml`, start the server, and confirm KBS logs
   `Latency provider: Grim transaction ping (PacketEvents fallback for untracked players)` with no linkage errors.
3. Join with a non-exempt Java player. If Hoplite is running Grim 3, run
   `/grim debug packetlog <player> --all` from console. Public Grim 2.0 does not provide this subcommand.
4. Have another player hit the player repeatedly for at least five seconds, then run
   `/knockbacksync ping <player>`. It must report a real ping and changing jitter rather than only an estimated ping.
5. On Grim 3, stop packet logging and run `rg '31407|31408'` against the reported packet log; it must return no
   matches. On every Grim version, the console/alerts must contain no new `BadPacketsO` violation for the player.
6. Run `/knockbacksync reload`, repeat steps 3-5, then relog the player and repeat once more. This checks listener
   replacement and both KBS/Grim player lifecycles.
7. On a separate clean dev-server copy, remove GrimAC, start the same KBS jar, and repeat the combat and
   `/knockbacksync ping` check. KBS must load without Grim classes and must resume PacketEvents measurements.

For Fabric, use the matching public Grim `2.0` Fabric build and run this equivalent smoke test:

1. Build with `./gradlew clean build`, stop the Fabric 26.2 server, and copy the generated KBS Fabric jar and the
   matching Grim Fabric jar into `mods/`. Remove any old per-server Grim packet exception.
2. Leave `latency.provider: "AUTO"` in `config/config.yml`, start the server, and confirm KBS logs
   `Latency provider: Grim transaction ping (PacketEvents fallback for untracked players)` with no linkage errors.
3. Join with a non-exempt player and have another player hit them repeatedly for at least five seconds.
   `/knockbacksync ping <player>` must report a real ping and changing jitter, and no new `BadPacketsO` alert may
   appear. Do not assume public Grim 2.0 has Grim 3's packet-log command.
4. Run `/knockbacksync reload`, repeat step 3, then relog and repeat once more.
5. On a separate clean server copy, remove Grim, start the same KBS jar, and repeat the combat/ping check. KBS must
   load without Grim API classes and resume its PacketEvents measurement path.
