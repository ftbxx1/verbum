# list & text helpers
command warps
    set player's warps::* to village and nether and end
    # text cases
    set player's shout to uppercase of hello
    tell player %player's shout%
    # join with a separator
    set player's route to join of player's warps::* by ,
    tell player Route: %player's route%
    # a random stop on the route
    set player's stop to random element of player's warps::*
    tell player Go to %player's stop%
    # order helpers
    set player's sorted::* to alphabetically sorted player's warps::*
    set player's back::* to reversed player's warps::*
    # size and index info
    if size of player's warps::* is at least 3
        set player's stops to indices of player's warps::*
        tell player Stops: %player's stops%
    # first and last
    set player's first to first of the list player's warps::*
    set player's last to last of the list player's warps::*
    tell player First is %player's first% and last is %player's last%