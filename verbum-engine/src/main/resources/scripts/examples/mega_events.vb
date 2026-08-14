# mega_events.vb — the big event vocabulary, ordering, cancel, and async

# Handlers run highest priority first. cancel event stops the rest.
when player joins priority high
    cancel event
    announce A VIP needs silence now

when player joins
    tell player Welcome

when player quits
    announce Goodbye

when player says hi
    tell player Hello to you too

when player teleports to Spawn
    announce Back to spawn

when player touches water
    tell player No swimming here

when player catches fire
    cancel event
    tell player I saved you, no burning!

when player swims in lava
    tell player That is a bad idea

# Run something a little later without freezing the script.
command boost
    tell player Speed boost coming up
    after 5 seconds
        give player 1 speed_boost