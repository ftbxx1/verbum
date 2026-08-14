# Verbum — command reference

Every Verbum verb, what it does, and an example. Words in *italics* are things
you fill in.

## Trigger words (start a block)

| Trigger            | Meaning                              |
|--------------------|--------------------------------------|
| `when <condition>` | run the block when the situation happens |
| `every <n> seconds\|minutes\|hours` | run the block on a clock |
| `on server start`  | run once when the server starts      |
| `on server stop`   | run once when the server stops       |
| `action <name> ...`| define a reusable action             |

## Chat and messages

| Command | Meaning | Example |
|---------|---------|---------|
| `tell` | private message | `tell player Welcome` |
| `warn` | red warning   | `warn player Low health` |
| `announce` | message to everyone | `announce New day` |
| `title` | big centred text | `title player with FIGHT` |
| `toast` | toast notification | `toast player Nice` |
| `welcome` | join greeting | `welcome player with Hello` |

## Items and inventory

| Command | Meaning | Example |
|---------|---------|---------|
| `give` | add items | `give player 5 diamonds` |
| `take` | remove items | `take 1 emerald from player` |
| `clear` | remove all of an item | `clear player's stick` |

## Life and body

| Command | Meaning | Example |
|---------|---------|---------|
| `kill` | set health to zero | `kill player` |
| `damage` | hurt | `damage player by 10` |
| `heal` | restore some health | `heal player by 5` |
| `heal to full` | restore all health/hunger | `heal player to full` |
| `ignite` | set on fire | `ignite player for 5 seconds` |
| `freeze` | freeze | `freeze player` |

## Movement

| Command | Meaning | Example |
|---------|---------|---------|
| `teleport ... to` | move a player | `teleport player to home` |
| `set fly` | allow flying | `set fly player` |
| `set walk speed` | change speed | `set walk speed player 30` |

## World

| Command | Meaning | Example |
|---------|---------|---------|
| `set weather` | rain/sun/storm | `set weather to rain` |
| `set time` | day/night/noon/midnight | `set time to night` |
| `set block` | place a block | `set block stone at 10 64 10` |
| `break block` | remove a block | `break block at 10 64 10` |
| `play sound` | play a sound | `play sound player level up` |
| `play particle` | show particles | `play particle player heart` |
| `lightning` | lightning bolt | `lightning at player` |

## Creatures

| Command | Meaning | Example |
|---------|---------|---------|
| `spawn` | create mobs | `spawn zombie` or `spawn 5 zombies` |
| `despawn` | remove mobs | `despawn zombie` |
| `set mob health` | change mob health | `set mob health boss to 200` |
| `set mob speed` | change mob speed | `set mob speed boss to 0.5` |
| `give effect` | potion effect | `give player night vision for 30 seconds` |
| `enchant` | enchant an item | `enchant player's sword with sharpness level 3` |

## Doors, gates, game

| Command | Meaning | Example |
|---------|---------|---------|
| `open door` | open a door | `open trading door` |
| `close door` | close a door | `close trading door` |
| `open gate` / `close gate` | gates | `open castle gate` |
| `win game` | declare a winner | `win game` |
| `lose game` | declare a loser | `lose game` |
| `give XP` | experience | `give player 100 XP` |
| `give levels` | levels | `give levels player 1` |
| `set gamemode` | change mode | `set gamemode player to creative` |

## Server administration

| Command | Meaning | Example |
|---------|---------|---------|
| `ban` | ban a player | `ban player` |
| `kick` | kick a player | `kick player bye` |
| `give permission` | grant permission | `give permission player verbum.fly` |
| `remove permission` | revoke permission | `remove permission player verbum.fly` |

## Variables

| Command | Meaning | Example |
|---------|---------|---------|
| `set ... to` | set a value | `set player's coins to 100` |
| `add ... to` | add to a value | `add 10 to player's coins` |
| `remove ... from` | subtract | `remove 5 from player's coins` |
| `multiply ... by` | multiply | `multiply player's coins by 2` |
| `divide ... by` | divide | `divide player's coins by 4` |
| `increase ... by` | +1 by default | `increase player's coins by 1` |
| `decrease ... by` | -1 by default | `decrease player's coins by 1` |
| `save ... to database` | keep forever | `save player's coins to database` |
| `load ...` | bring back | `load player's coins` |
| `after N seconds:` | run this block later | `after 5 seconds` then a block |
| `cancel event` | stop later event handlers | `cancel event` |
| `priority high/low` | event handler order | `when player joins priority high` |

## Regions

| Command | Meaning | Example |
|---------|---------|---------|
| `define area <name> <x1> <z1> <x2> <z2>` | mark an area | `define area Castle 0 0 100 100` |
| `define area <name> <x1> <y1> <z1> <x2> <y2> <z2>` | 3D area | `define area Boss Room 300 0 300 350 200 350` |

## Flow control

| Word | Meaning |
|------|---------|
| `if ... else if ... else` | choose a branch |
| `repeat <n> times` | loop a set number |
| `repeat while <condition>` | loop while true |
| `until <condition>` | loop until true |
| `for each X in LIST` | loop over things |
| `break` | stop this loop |
| `continue` | next turn of this loop |
| `stop` | stop everything now |
