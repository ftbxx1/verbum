# boss_fight.vb — a multi-stage boss using a custom action

action spawn the boss
    spawn boss
    set mob health boss to 200
    announce A boss has appeared

action reward the winner
    give player 1 dragon egg
    give player 500 XP
    announce The boss has been defeated
    tell player You are a hero

when player enters area boss room
    spawn the boss

when boss is at half health
    announce The boss is angry
    give player fire resistance
    ignite player for 3 seconds

when boss dies
    reward the winner
