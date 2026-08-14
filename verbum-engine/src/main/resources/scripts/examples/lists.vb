# lists.vb — Skript-style {list::*} variables.
# Collect with  set {list::*} to a and b  or  add X to {list::*},  loop with
#  for each X in {list::*},  and read or write single spots with  {list::1}.
# {list::name} entries work like a dictionary. This is the pattern real
# servers use for warps, shops, quests and kits.

# /addwarp Village   — save a warp to your personal list
command addwarp name
    if player's warps::* contains name
        tell player You already saved %name%
    else
        add name to player's warps::*
        tell player Saved %name%!

# /warps   — number your saved warps and show them
command warps
    if player's warps::* is empty
        tell player No warps saved yet
    else
        tell player Your warps:
        for each w in player's warps::*
            tell player %loop-index%. %w%

# /lastwarp   — read a single spot back out of the list
command lastwarp
    if player's warps::* is not empty
        set player's last to player's warps::1
        tell player Your first warp is %player's last%

# /setupwarps   — declare the places the server knows (run once)
command setupwarps
    define area Village
    define area Nether

# /goto Village   — jump to a saved warp
command goto name
    if player's warps::* contains name
        teleport player to name
        tell player Teleported to %name%
    else
        tell player I have no warp named %name%

# A global price catalogue: {price::ITEM} works like a dictionary
command catalog
    set {price::sword} to 100
    set {price::bow} to 50
    if {price::bow} is set
        tell player A bow costs %{price::bow}% coins