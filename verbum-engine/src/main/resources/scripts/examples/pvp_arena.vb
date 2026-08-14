# pvp_arena.vb — a simple arena with rewards

on server start
    define area arena 0 0 100 100
    define area winner room 200 0 200 250 200 250

when player enters area arena
    title player with FIGHT
    give player iron sword
    heal player to full

when player dies in the arena
    lose game

when player kills a player
    add 1 to player's kills
    tell player Nice fight

when player's kills are at least 3
    announce Champion
    teleport player to winner room
    remove 3 from player's kills
