# Verbum - command reference

Generated from the interpreter source so it always matches what the server
actually understands. Verbs are the first word of a line inside a block.

> **273 verbs, 113 event words, 102 language keyword fallbacks.**

## Trigger words (start a block)

| Trigger | Meaning |
|---------|---------|
| `when <condition>` | run the block when the situation happens |
| `every <n> seconds|minutes|hours` | run the block on a clock |
| `on server start` / `on server stop` | run once when the server starts/stops |
| `action <name> <params>` | define a reusable action (function) |
| `command <name>` | define a custom chat command, e.g. `/hello` |
| `menu <name>` | define a clickable inventory menu |


## Third-party plugins

| Verb | You can also write |
|------|--------------------------|
| `plugin` |  |

## Variables & math

| Verb | You can also write |
|------|--------------------------|
| `add` |  |
| `call` |  |
| `cooldown` |  |
| `decrease` |  |
| `divide` |  |
| `increase` |  |
| `let` |  |
| `load` |  |
| `lore` |  |
| `modeldata` |  |
| `multiply` |  |
| `remove` |  |
| `rename` |  |
| `save` |  |
| `set` |  |

## Messages

| Verb | You can also write |
|------|--------------------------|
| `actionbar` |  |
| `announce` |  |
| `broadcast` |  |
| `clearchat` |  |
| `disableprivatechat` |  |
| `disablepublicchat` | `hidechat` |
| `message` |  |
| `say` |  |
| `sendclickmessage` |  |
| `sendhovermessage` |  |
| `setjoinmessage` |  |
| `setprivatechat` |  |
| `setpublicchat` |  |
| `setquitmessage` |  |
| `tell` |  |
| `title` |  |
| `toast` |  |
| `warn` |  |
| `welcome` |  |

## Inventory

| Verb | You can also write |
|------|--------------------------|
| `clear` |  |
| `drop` |  |
| `give` |  |

## Life

| Verb | You can also write |
|------|--------------------------|
| `damage` |  |
| `heal` |  |
| `kill` |  |

## Movement

| Verb | You can also write |
|------|--------------------------|
| `teleport` |  |

## World/state

| Verb | You can also write |
|------|--------------------------|
| `apply` |  |
| `despawn` |  |
| `effect` |  |
| `enchant` |  |
| `explode` |  |
| `feed` |  |
| `freeze` |  |
| `ignite` |  |
| `lightning` |  |
| `particle` |  |
| `playsound` |  |
| `setblock` |  |
| `smite` |  |
| `spawn` | `spawnmob` |
| `strike` | `lightning`, `thunder` |
| `time` |  |
| `unenchant` |  |
| `weather` |  |

## Doors / game

| Verb | You can also write |
|------|--------------------------|
| `close` |  |
| `lose` |  |
| `open` |  |
| `win` |  |

## Economy

| Verb | You can also write |
|------|--------------------------|
| `balance` |  |
| `charge` |  |
| `deposit` |  |
| `pay` |  |
| `withdraw` |  |

## Admin

| Verb | You can also write |
|------|--------------------------|
| `ban` |  |
| `cancel` |  |
| `cancelfalldamage` |  |
| `deop` |  |
| `gamemode` |  |
| `givepermission` |  |
| `glowing` |  |
| `gravity` |  |
| `hide` |  |
| `invisible` |  |
| `kick` |  |
| `make` |  |
| `makesneak` |  |
| `makesprint` |  |
| `mute` |  |
| `op` |  |
| `removepermission` |  |
| `reset` |  |
| `setabsorption` |  |
| `setarmor` |  |
| `setarmorpoints` |  |
| `setcooldown` |  |
| `setfly` | `fly` |
| `setinvincible` |  |
| `setmobhealth` |  |
| `setmobspeed` |  |
| `setsneaking` |  |
| `setsprinting` |  |
| `setwalkspeed` |  |
| `show` |  |
| `unvanish` |  |
| `vanish` |  |
| `visible` |  |

## Inventory & slots

| Verb | You can also write |
|------|--------------------------|
| `put` |  |
| `setitemamount` |  |
| `setskullowner` |  |
| `setslot` |  |
| `setunbreakable` |  |
| `swap` |  |

## Player tuning

| Verb | You can also write |
|------|--------------------------|
| `cleareffects` |  |
| `feedfull` |  |
| `fullheal` |  |
| `launch` |  |
| `setarmorslot` |  |
| `setattackspeed` |  |
| `setdisplayname` |  |
| `setflying` |  |
| `setfoodlevel` |  |
| `setgliding` |  |
| `setglowcolor` |  |
| `setlistname` |  |
| `setrespawn` |  |
| `settablistheader` |  |
| `unfreeze` |  |

## Broadcasts

| Verb | You can also write |
|------|--------------------------|
| `broadcastactionbar` |  |
| `broadcastsound` |  |
| `broadcasttitle` |  |
| `broadcasttoast` |  |
| `musicdisc` |  |
| `stopsounds` |  |

## World effects (coordinate based)

| Verb | You can also write |
|------|--------------------------|
| `dropat` |  |
| `dropexperience` |  |
| `fill` |  |
| `lightningat` |  |
| `playsoundat` |  |
| `randomitem` |  |

## Xp / levels

| Verb | You can also write |
|------|--------------------------|
| `experience` |  |
| `levels` |  |

## Regions

| Verb | You can also write |
|------|--------------------------|
| `define` |  |

## Scoreboards

| Verb | You can also write |
|------|--------------------------|
| `addscore` |  |
| `createscoreboard` |  |
| `deletescoreboard` |  |
| `removescore` |  |
| `setscore` |  |
| `setscoreboarddisplay` |  |

## Teams

| Verb | You can also write |
|------|--------------------------|
| `addtoteam` |  |
| `createteam` |  |
| `removefromteam` |  |
| `setteamcolor` |  |
| `setteamprefix` |  |
| `setteamsuffix` |  |

## Boss bars

| Verb | You can also write |
|------|--------------------------|
| `createbossbar` |  |
| `hidebossbar` |  |
| `removebossbar` |  |
| `setbossbar` |  |
| `setbossbarcolor` |  |
| `setbossbarprogress` |  |
| `setbossbarstyle` |  |
| `showbossbar` |  |

## World extras

| Verb | You can also write |
|------|--------------------------|
| `setborder` |  |
| `setdifficulty` |  |
| `setexplosiondamage` |  |
| `setmoblimit` |  |
| `setplayerlimit` |  |
| `setredstone` |  |
| `setspawnpoint` |  |
| `setstorm` |  |
| `setthunder` |  |
| `settimespeed` |  |
| `setweatherduration` |  |
| `setworldrule` |  |
| `spawnstructure` |  |

## Item & armor

| Verb | You can also write |
|------|--------------------------|
| `clearinventory` |  |
| `givearmor` |  |
| `givehelmet` |  |
| `renameitem` |  |
| `sethelditem` |  |
| `setitemflag` |  |
| `setlore` |  |
| `setmodeldata` |  |
| `setoffhanditem` |  |

## Guis

| Verb | You can also write |
|------|--------------------------|
| `openanvil` |  |
| `openinventory` |  |
| `openmenu` |  |
| `openshop` |  |
| `openworkbench` |  |

## Projectiles

| Verb | You can also write |
|------|--------------------------|
| `adddeath` |  |
| `addkill` |  |
| `shoot` |  |
| `shootcoloredfirework` |  |
| `shootfirework` |  |
| `throw` |  |

## Mob extras

| Verb | You can also write |
|------|--------------------------|
| `makemob` |  |
| `namemob` |  |
| `setmobage` |  |
| `setmobai` |  |
| `setmobbreeding` |  |
| `setmobdrop` |  |
| `setmobflying` |  |
| `setmobfollow` |  |
| `setmobgravity` |  |
| `setmobnamevisible` |  |
| `setmobpersistent` |  |
| `setmobpitch` |  |
| `setmobsize` |  |
| `setmobtarget` |  |
| `tamemob` |  |

## Server extras

| Verb | You can also write |
|------|--------------------------|
| `revive` |  |
| `sendtolobby` |  |
| `sendtoserver` |  |
| `setclickcommand` |  |
| `setmute` |  |
| `setsigntext` |  |
| `settablist` |  |
| `unban` |  |
| `unmute` |  |

## Flags & quests

| Verb | You can also write |
|------|--------------------------|
| `completequest` |  |
| `setflag` |  |
| `setquest` |  |
| `toggleflag` |  |

## Game flow

| Verb | You can also write |
|------|--------------------------|
| `endgame` |  |
| `nextround` |  |
| `setround` |  |
| `startgame` |  |

## More phrasal verbs

| Verb | You can also write |
|------|--------------------------|
| `breakblock` |  |
| `buy` |  |
| `closedoor` |  |
| `closegate` |  |
| `closetrapdoor` |  |
| `executecommand` |  |
| `extinguish` |  |
| `givelevels` |  |
| `givexp` |  |
| `healtofull` |  |
| `makeplayer` |  |
| `opendoor` |  |
| `opengate` |  |
| `opentrapdoor` |  |
| `removeeffect` |  |
| `sell` |  |
| `setflyspeed` |  |
| `setfood` |  |
| `setfrozen` |  |
| `setgamemode` |  |
| `setglowing` |  |
| `setgravity` |  |
| `sethealth` |  |
| `setinvisible` |  |
| `setinvulnerable` |  |
| `setitem` |  |
| `setitemflags` |  |
| `setlevel` |  |
| `setmaxhealth` |  |
| `setmobhostility` |  |
| `setmoney` |  |
| `setop` |  |
| `setshopprice` |  |
| `setsidebarline` |  |
| `setsidebartitle` |  |
| `setvillagerprice` |  |
| `setvisible` |  |
| `strikelightning` |  |
| `subtitle` |  |

## Depth batch: jail, homes, riding, repair, holograms, whitelist

| Verb | You can also write |
|------|--------------------------|
| `dismount` |  |
| `hologram` |  |
| `home` |  |
| `jail` |  |
| `removehologram` |  |
| `repair` |  |
| `ride` |  |
| `sethome` |  |
| `unjail` |  |
| `unwhitelist` |  |
| `whitelist` |  |

## Event words (what `when player ...` understands)

These are the situations `when` can react to, written as plain words:

| Kind | Example phrase |
|------|----------------|
| `advancement` | when player gets an advancement |
| `armorchange` | when player changes armor |
| `armorstand` | when player edits an armor stand |
| `arrow` | when player gets shot |
| `ban` | when player gets banned |
| `block` | when player blocks an attack |
| `bookedit` | when player edits a book |
| `break` | when player breaks diamond ore |
| `breed` | when player breeds animals |
| `brew` | when player brews a potion |
| `bucketcatch` | when player captures a fish |
| `bucketempty` | when player empties a bucket |
| `bucketfill` | when player fills a bucket |
| `burn` | when player burns |
| `button` | when player presses a button |
| `chat` | when player chats / says / types |
| `close` | when player closes a door |
| `collect` | when player collects emerald |
| `command` | when player uses command |
| `complete` | when player completes a quest |
| `consume` | when player drinks a potion |
| `craft` | when player crafts an item |
| `craftstart` | when player starts crafting |
| `damage` | when player damages a mob |
| `day` | when player day starts |
| `death` | when player dies / gets killed by |
| `dismount` | when player dismounts |
| `drop` | when player drops an item |
| `drown` | when player drowns |
| `eat` | when player eats food |
| `eggthrow` | when player throws an egg |
| `enchant` | when player enchants a sword |
| `enter` | when player enters area |
| `explosion` | when player a creeper explodes |
| `fall` | when player takes fall damage |
| `fire` | when player burns to death |
| `firstjoin` | when player first joins |
| `fish` | when player fishes |
| `freeze` | when player gets frozen |
| `gamemodechange` | when player changes gamemode |
| `harvest` | when player harvests crops |
| `heal` | when player heals / regains health |
| `hit` | when player hits a zombie |
| `hurt` | when player gets hurt / takes damage |
| `ignite` | when player catches fire |
| `inventoryclick` | when player clicks a slot |
| `invopen` | when player opens his inventory |
| `itembreak` | when player breaks a tool |
| `itemdamage` | when player damages an item |
| `join` | when player joins |
| `jump` | when player jumps |
| `kick` | when player gets kicked |
| `kill` | when player kills a mob |
| `land` | when player lands |
| `leave` | when player leaves area |
| `leftclick` | when player left clicks |
| `levelup` | when player levels up |
| `lever` | when player flips a lever |
| `lightning` | when player is struck by lightning |
| `lose` | when player loses game |
| `middleclick` | when player middle clicks |
| `milk` | when player milks a cow |
| `move` | when player moves to / steps on |
| `night` | when player night starts |
| `note` | when player plays a note |
| `open` | when player opens a chest |
| `pickup` | when player picks up a diamond |
| `piston` | when player a piston extends |
| `pistonretract` | when player a piston retracts |
| `place` | when player places a block |
| `plant` | when player plants a seed |
| `poison` | when player gets poisoned |
| `portal` | when player enters a portal |
| `portalexit` | when player exits a portal |
| `prime` | when player primes a tnt |
| `projectilehit` | when player a projectile hits |
| `quit` | when player quits |
| `raid` | when player starts a raid |
| `raidwin` | when player wins a raid |
| `rainstart` | when player it starts raining |
| `reach` | when player reaches area |
| `respawn` | when player respawns |
| `ride` | when player rides a horse |
| `rightclick` | when player right clicks |
| `shear` | when player shears a sheep |
| `shoot` | when player shoots a bow |
| `sleep` | when player goes to bed |
| `smelt` | when player a furnace smelts |
| `smith` | when player smiths an item |
| `sneak` | when player starts sneaking |
| `sprint` | when player starts sprinting |
| `start` | when player starts playing |
| `starve` | when player starves |
| `stormstart` | when player storm starts |
| `swap` | when player swaps hands |
| `swim` | when player starts swimming |
| `switch` | when player switches slot |
| `tame` | when player tames a pet |
| `teleport` | when player teleports to |
| `thunder` | when player thunder strikes |
| `toggleflight` | when player toggles flying |
| `togglesneak` | when player toggles sneak |
| `totem` | when player uses a totem |
| `trade` | when player trades with villager |
| `unsneak` | when player stops sneaking |
| `unsprint` | when player stops sprinting |
| `use` | when player uses an item |
| `void` | when player falls into void |
| `wake` | when player wakes up |
| `win` | when player wins game |
| `wither` | when player gets withered |
| `worldchange` | when player changes worlds |
| `xp` | when player gains experience |

## Flow control

| Word | Meaning |
|------|---------|
| `if ...` `else if ...` `else` | choose a branch |
| `repeat <n> times` | loop a set number |
| `repeat while <cond>` / `until <cond>` | loop while/until true |
| `for each <x> in <list>` | loop over a list |
| `break` | stop this loop |
| `continue` | next turn of this loop |
| `stop` | stop the whole handler |
| `wait`, `after <n> seconds:` | pause and run later |
| `cancel event` | stop later handlers for this event |
| `priority high` / `priority low` | event handler order |

---

Lines that start with `note` or `#` are comments and are ignored.
