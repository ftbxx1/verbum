# loops.vb — useful loops

on server start
    set player's gold to 0

every 10 seconds
    add 1 to player's gold
    tell player You earned a piece of gold

when player uses command loot
    repeat 5 times
        give player emerald
    repeat while player has less than 10 gold
        add 1 to player's gold
    for each online player
        give player 1 diamond
    until player's gold is at least 20
        add 1 to player's gold
