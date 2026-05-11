# No Way Up

Forge 1.20.1 psychological horror mod prototype.

## Current Prototype

- Spawns first-time players inside a sealed mineshaft chamber.
- Tracks fear progression per player with world `SavedData`.
- Plays unsettling vanilla sound cues around the player.
- Sends English horror whispers and subtitle-style action bar messages.
- Creates a one-time desktop text file: `READ_ME_NOWAYUP.txt`.
- Triggers a one-time automatic forced crash during mid-game progression to lure the player into reconnecting.
- Places English story/lore books across separate starting mine chests.
- Spawns a temporary watcher illusion behind or ahead of the player using the player's head and dark worn armor.
- Redirects upward escape attempts into newly generated deeper mineshaft segments.
- Generates varied mine segments: storage rooms, bunks, cave-ins, sculk shrines, split rails, false exits, signs, and message chests.
- Mutates the starting mine after reconnects: cold torches, opened doors, new signs, and message books.
- Registers and uses the `nowayup:mirror_mine` dimension for the mirrored mine.
- Runs staged mirror collapse events with block changes, sound, darkness, and route closure.
- Supports Loop, Descent, and Replacement endings with world changes and status effects.
- Suppresses monster spawns inside the mirror mine.

## Horror File Note

During normal progression the mod creates `READ_ME_NOWAYUP.txt` on the user's Desktop once. It never creates executables, scripts, startup entries, or overwrites existing files.

## Story

The full story, timeline, lore notes, and ending concepts are in [STORY.md](STORY.md).
Korean translation is available in [STORY_KO.md](STORY_KO.md).

## Test Commands

Requires operator permission:

```text
/nowayup start
/nowayup fear get
/nowayup fear add 160
/nowayup fear set 220
/nowayup desktop
/nowayup crash
/nowayup watcher
/nowayup lore
/nowayup mutate
/nowayup mirror
```

Suggested quick test:

```text
/nowayup start
/nowayup mutate
/nowayup fear set 260
/nowayup watcher
/nowayup watcher
/nowayup watcher
/nowayup mirror
```

The mod can intentionally throw a real runtime exception during progression. `/nowayup crash` triggers the same style of crash manually for testing.

## Build

```powershell
./gradlew build
```

Network access is required the first time Gradle downloads Forge.
