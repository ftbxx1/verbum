# conditions.vb — learn if, else, and joining ideas

when player joins
    set player's score to 0

every 3 seconds
    if player's score is at least 10 and it is daytime
        announce High score in the day
    else if player is sprinting or it is nighttime
        announce Moving fast or in the dark
    else
        announce Nothing special

when player health is below 5
    warn player You are low on health

when player is above y 60
    give player slow falling
