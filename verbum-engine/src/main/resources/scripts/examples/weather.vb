# weather.vb — control time and weather

on server start
    set time to day
    set weather to sunny

every 5 seconds
    if it is nighttime
        spawn zombie
    else if it is daytime
        despawn zombie

when it is raining
    announce It is raining

when player uses command night
    set time to night

when player uses command day
    set time to day
