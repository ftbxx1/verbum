# Installing Verbum on a Paper server

Verbum v1 runs on a **Paper** server (Paper 1.21.11, API 1.21.11-R0.1-SNAPSHOT).

## 1. Get a Paper server

Put Paper on your machine:
- Download Paper from https://papermc.io/downloads (the `.jar` for your version).
- Run `java -jar paper-1.21.11.jar` once to create the server folder.
- Put `eula=true` in `eula.txt`.

## 2. Install the plugin

Grab the latest `verbum-paper-*.jar` from the [Releases page](https://github.com/ftbxx1/verbum/releases)
(or build from source: `mvn package`, then copy it from `verbum-paper/target/`)
into your server's `plugins/` folder.

## 3. Add your scripts

- Start the server once, then stop it. Verbum creates
  `plugins/Verbum/scripts/`.
- Drop your `.vb` files in `plugins/Verbum/scripts/`
  (a sample `game.mcscript` is created for you).

## 4. Reload

- Start the server, or run `/verbum reload` in-game after editing scripts.
- Server console will tell you how many scripts loaded.

## 5. Test it

- Join and type a command, walk into water, or use one of the included example
  scripts. The plugin bridges your `.vb` rules straight into the live world.

That's it — five steps. Editing a script is just: change a file, `/verbum reload`.

---

## Running it WITHOUT a Minecraft server

To prove the language works with no server at all (offline mock world):

```
java -jar verbum-engine.jar check  game.mcscript    # validate a file
java -jar verbum-engine.jar run    game.mcscript    # run a small demo
java -jar verbum-engine.jar demo                    # acceptance test -> PASS
```

To run the automated test suite:

```
mvn package                  # builds both modules and runs all tests
powershell -File run-tests.ps1
```
