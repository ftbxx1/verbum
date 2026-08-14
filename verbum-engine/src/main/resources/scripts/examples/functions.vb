# functions.vb — functions with return values, cooldowns, {brace} variables,
# math helpers and item metadata. This is what Skript fans are used to.

# A function that returns a value. Call it from anywhere:  call name with a and b
function double number
    return number * 2

function bigger a and b
    if a is greater than b
        return a
    return b

function balance player
    set {balance:player} to 0
    return {balance:player}

# Use functions, braces and cooldowns together: /shop
command shop
    cooldown 3 seconds
    set player's price to double 25
    set world last-price to bigger 45 and 40
    tell player The sword costs %world last-price% coins

# Braces make Skript variables work like at home: {coins}, {player's gold}, {world total}
command stash
    set {coins} to 5
    add 3 to {coins}
    set {player's gold} to {coins}
    if {coins} is 8
        tell player You have %player's gold% coins in a brace variable

# Math helpers you can use anywhere a number is expected
command mathy
    set player's a to floor of 4.7
    set player's b to ceil of 4.2
    set player's c to round of 4.5
    set player's d to absolute value of -3
    set player's e to sqrt of 16
    set player's f to max of 3 and 9
    set player's g to min of 3 and 9
    tell player floor %player's a% ceil %player's b% max %player's f%

# Item metadata: /upgrade your sword
command upgrade
    rename player's sword to Epic Sword
    lore player's sword to Shiny and Sharp and Legendary
    modeldata player's sword to 100
    tell player Your sword was upgraded

# Check the weather like a storm-chaser
command weathercheck
    if it is thundering
        warn player Lightning season
    if it is raining
        tell player Bring an umbrella