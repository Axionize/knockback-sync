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
- `fabric` contains code for latest fabric version (currently 1.21)
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
- Runnable and manual ping does not work on fabric right now due b/c packetevents. Ping measurement will be less accurate

## Grim latency integration verification

`GrimLatencyIntegrationHarnessTest` uses the published Grim API's real typed event-channel classes. It fires a
`GrimTransactionReceivedEvent` through the production Grim provider and verifies that the sample reaches KBS,
that a tracked user receives no KBS synthetic ping, that the platform ping remains the fallback before Grim's
first sample, and that join, quit, reload/replacement, and disable/unsubscribe behavior is preserved. This proves
the Grim API event-to-KBS state path without requiring a Minecraft client.

The harness cannot prove plugin classloader linkage on a particular server build or observe the final Netty packet
stream. Run this Hoplite dev-server smoke test before deployment:

1. Build with `./gradlew clean build`, stop Hoplite, remove the old KnockbackSync jar, and copy
   `bukkit/build/libs/knockbacksync-bukkit-*.jar` into `plugins/`. Remove the old per-server Grim packet exception.
2. Leave `latency.provider: "AUTO"` in `plugins/KnockbackSync/config.yml`, start the server, and confirm KBS logs
   `Latency provider: Grim transaction ping (PacketEvents fallback for untracked players)` with no linkage errors.
3. Join with a non-exempt Java player and run `/grim debug packetlog <player> --all` from console.
4. Have another player hit the logged player repeatedly for at least five seconds, then run
   `/knockbacksync ping <player>`. It must report a real ping and changing jitter rather than only an estimated ping.
5. Run `/grim debug packetlog <player> --all` again to stop logging. In the reported file under
   `plugins/GrimAC/packetlogs/`, `rg '31407|31408' <file>` must return no matches, and the console/alerts must contain
   no new `BadPacketsO` violation for the test player.
6. Run `/knockbacksync reload`, repeat steps 3-5, then relog the player and repeat once more. This checks listener
   replacement and both KBS/Grim player lifecycles.
7. On a separate clean dev-server copy, remove GrimAC, start the same KBS jar, and repeat the combat and
   `/knockbacksync ping` check. KBS must load without Grim classes and must resume PacketEvents measurements.
