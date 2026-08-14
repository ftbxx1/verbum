# commands.vb — respond to chat and typed commands

on server start
    announce Server is up

when player joins
    tell player Welcome player

when player types (home)
    teleport player to home base

when player uses command feed
    heal player to full
    tell player You are full

when player uses command fly
    set fly player
    tell player You can fly now

when player chats
    announce player said something
