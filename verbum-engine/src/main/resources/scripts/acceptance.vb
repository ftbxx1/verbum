# ------------------------------------------------------------------
# Acceptance test — proves the whole pipeline works end to end.
# A server owner installs the plugin and writes this file, and it
# works in game. This copy runs against the offline mock runtime too.
# ------------------------------------------------------------------

on server start
    define area victory area 0 0 200 200
    define area castle 0 0 50 50
    define area boss room 300 0 300 350 200 350
    define area exit 500 0 500 550 200 550

when player touches water
    kill player

when player collects emerald
    add 1 to player's emeralds

when player has 10 emeralds
    announce Player Wins
    teleport player to victory area

when boss dies
    give all players 1 dragon egg
