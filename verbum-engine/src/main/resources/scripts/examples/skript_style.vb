# skript_style.vb — custom commands, GUIs, waits and economy,
# the things players love most about Skript-style scripting.

# A custom command: type /hello in-game.
command hello
    tell player Hello from Verbum
    announce player just said hello

# A command with an argument: /greet Steve
command greet name
    tell player Hello name

# A command that opens a menu (GUI): /menu
command menu
    open menu rewards

# The GUI itself. Clicking a button runs its little block.
menu rewards
    add button Unlock Sword
        take 100 coins from player
        give player diamond sword
        tell player You bought a sword
    add button Free Bread
        give player bread
    add button Heal Up
        heal player to full
        tell player All healed

# A command with a delay / cooldown: /boom
command boom
    tell player Stand back...
    wait 2 seconds
    announce BOOM
    lightning at player

# A command using the economy helpers: /money
command money
    balance player

command earn
    pay player 50
    tell player You earned 50 coins

every 5 seconds
    add 1 to player's coins

when player joins
    set player's coins to 0
    tell player Type /menu to open the rewards menu
