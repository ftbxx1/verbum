# money.vb — a little economy

on server start
    set world new players to 0

when player joins
    add 1 to world new players

when player breaks diamond ore
    add 100 to player's coins
    announce Player earned coins

when player has 1000 coins
    give player emerald
    remove 1000 from player's coins
    tell player You bought an emerald

when player uses command sell
    add 10 to player's coins
    tell player Sold for 10 coins
