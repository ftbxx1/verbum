# quests.vb — a friendly quest chain

when player joins
    set player's quest to start

when player collects 10 wood
    add 1 to player's quest
    tell player You finished the first step

when player's quest is at least 2
    give player iron armor
    announce Quest complete
    set player's quest to done

when player wins game
    give player 100 XP
    give levels player 1
