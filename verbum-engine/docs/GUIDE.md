# Verbum — beginner guide

Verbum lets you tell Minecraft what to do in plain English. You do not need to
know any programming. This guide gets you writing working scripts in a few
minutes.

## How a script is built

A script is a list of **"when something happens, do this"** rules.

```
when player joins
    tell player Welcome to the server
```

- Line 1 is the **trigger** (the thing that happens).
- The indented lines are the **actions** (what to do).
- The indentation is what groups the actions with their trigger.

## Your first script

Create a file called `hello.vb`:

```
when player joins
    tell player Hello player
    give player a bread

when player touches water
    kill player
```

Save it in `plugins/Verbum/scripts/` and run `/verbum reload`.
Now when anyone joins they get a hello and a bread, and anyone touching water
is killed (a classic trap).

## Actions you will use most

| You want to...                   | Write                          |
|----------------------------------|--------------------------------|
| Say something to a player        | `tell player Hello there`      |
| Say something to everyone        | `announce Welcome everyone`    |
| Give an item                     | `give player 5 diamonds`       |
| Take away                        | `take 1 diamond from player`   |
| Hurt                             | `damage player by 5`           |
| Heal                             | `heal player by 5`  or  `heal player to full` |
| Remove a player                  | `kill player`                  |
| Move a player                    | `teleport player to home`      |
| Create something                 | `spawn zombie`                 |
| Change the sky                   | `set weather to rain`          |
| Change the time                  | `set time to night`            |

## Remembering numbers (variables)

Make the server keep score for you:

```
when player breaks diamond ore
    add 10 to player's coins

when player has 100 coins
    give player 1 diamond
    tell player You saved up for a diamond
```

`player's coins` is **that player's** own score. Other players each have their
own. Use `world highscore` for something whole-server.

## Making choices

```
every 5 seconds
    if player health is below 5
        warn player You are almost dead
    else if it is nighttime
        announce Someone is out at night
    else
        announce All is well
```

## Repeating things

```
when player uses command loot
    repeat 5 times
        give player 1 emerald
    for each online player
        give player 1 emerald
```

## Write your own action

Give a group of lines a name to reuse them:

```
action reward player
    give player 10 diamonds
    announce A reward was given

when player completes quest
    reward player
```

## Handy tips

- **Indent with 4 spaces.** Consistency matters more than the exact number, as
  long as nested lines are deeper than the line they belong to.
- **No punctuation.** Never use quotes, commas or plus signs.
- **Every error tells you what to write instead.** Just read the message.
- Put one script per `.vb` file in `plugins/Verbum/scripts/`, then `/verbum reload`.

## Common plans

- **An adventure:** `define area Castle`, then reward players who `when player
  reaches area Castle`.
- **A shop:** on a command, `take 10 coins from player` and `give player
  emerald`.
- **A boss:** `action spawn the boss` → `spawn boss`; reward the winner
  `when boss dies`.

Start small. One trigger, a couple of actions. Then grow.
