package dev.verbum.interp;

import dev.verbum.ast.ActionCall;
import dev.verbum.error.VerbumError;
import dev.verbum.runtime.McRuntime;
import dev.verbum.runtime.Location;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Turns Verbum action calls into Minecraft runtime calls.
 *
 * The verb is the first word; the remaining words are interpreted freely so
 * Verbum accepts multiple natural phrasings of the same idea.
 */
public final class Actions {

    private Actions() {}

    public static void execute(Interpreter it, String verb, List<String> a, int line) {
        McRuntime r = it.runtime();
        String v = canonical(verb);

        switch (v) {
            // ---- third-party plugins ---------------------------------------------
            case "plugin": it.runPluginLine(a, line); break;

            // ---- variables & math ------------------------------------------------
            case "set": if (!setCapability(it, a, r, line)) setVariable(it, a, line); break;
            case "let": setVariable(it, a, line); break;
            case "add": if (!addCapability(it, a, r, line)) addTo(it, a, line); break;
            case "remove": case "subtract": case "take":
                if (v.equals("take")) { takeAction(it, a, line); break; }
                if (!removeCapability(it, a, r, line)) removeFrom(it, a, line); break;
            case "multiply": multiply(it, a, line); break;
            case "divide": divide(it, a, line); break;
            case "increase": incdec(it, a, +1, line); break;
            case "decrease": incdec(it, a, -1, line); break;
            case "save": saveData(it, a, line); break;
            case "load": loadData(it, a, line); break;
            case "cooldown": cooldown(it, a, line); break;
            case "call": it.runFunctionCall(a, line); break;
            case "rename": renameItem(it, a, line); break;
            case "lore": loreItem(it, a, line); break;
            case "modeldata": case "custommodeldata": modelData(it, a, line); break;

            // ---- messages ---------------------------------------------------------
            case "tell": r.tell(target(it, a, 0), message(it, a, 1, "tell player")); break;
            case "message": r.tell(target(it, a, 0), message(it, a, 1, "message player")); break;
            case "say": r.tell(target(it, a, 0), message(it, a, 1, "say player")); break;
            case "warn": r.warn(target(it, a, 0), message(it, a, 1, "warn player")); break;
            case "announce": r.announce(interpText(it, a)); break;
            case "broadcast": r.announce(interpText(it, a)); break;
            case "welcome": r.tell(target(it, a, 0), textAfter(it, a, "with", "welcome player with message")); break;
            case "title": titleAction(it, a, line); break;
            case "toast": r.toast(target(it, a, 0), message(it, a, 1, "toast player")); break;
            case "actionbar": r.actionbar(target(it, a, 0), message(it, a, 1, "actionbar player")); break;
            case "setjoinmessage": {
                String m = textAfter(it, a, "to", "set join message to Welcome");
                if (m.isEmpty()) m = interpText(it, a);
                r.setJoinMessage(m);
                break;
            }
            case "setquitmessage": {
                String m = textAfter(it, a, "to", "set quit message to Bye");
                if (m.isEmpty()) m = interpText(it, a);
                r.setQuitMessage(m);
                break;
            }
            case "setpublicchat": r.setPublicChat(!(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false"))); break;
            case "disablepublicchat": r.setPublicChat(false); break;
            case "setprivatechat": r.setPrivateChat(!(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false"))); break;
            case "disableprivatechat": r.setPrivateChat(false); break;
            case "clearchat": r.clearChat(target(it, a, 0)); break;
            case "sendhovermessage": {
                int with = indexOfIgnoreCase(a, "with");
                int tip = indexOfIgnoreCase(a, "tooltip");
                String hover = tip >= 0 ? interpText(it, a.subList(tip + 1, a.size()))
                        : (with >= 0 ? interpText(it, a.subList(with + 1, a.size())) : "");
                String body = with >= 0 ? interpText(it, a.subList(1, with)) : message(it, a, 1, "send hover message");
                r.sendHoverMessage(target(it, a, 0), body, hover);
                break;
            }
            case "sendclickmessage": {
                int with = indexOfIgnoreCase(a, "with");
                int cmd = indexOfIgnoreCase(a, "command");
                int bodyEnd = with >= 0 ? with : cmd;
                String body = bodyEnd >= 0 ? interpText(it, a.subList(1, bodyEnd)) : message(it, a, 1, "send click message");
                String command = cmd >= 0 ? interpText(it, a.subList(cmd + 1, a.size())) : "";
                r.sendClickMessage(target(it, a, 0), body, command);
                break;
            }

            // ---- inventory --------------------------------------------------------
            case "give": giveAction(it, a, line); break;
            case "drop": dropAction(it, a, line); break;
            case "clear": case "delete": {
                String joined = VariableStore.join(a);
                boolean varLike = joined.contains("::") || joined.contains("{") || joined.contains("}");
                if (varLike) clearVar(it, v, a, line);
                else if (v.equals("clear")) r.clearItem(target(it, a, 0), text(a, 1, "clear item"));
                break;
            }

            // ---- life --------------------------------------------------------------
            case "kill": r.kill(target(it, a, 0)); break;
            case "damage": r.damage(target(it, a, 0), amount(it, a, 1, "damage player by 10")); break;
            case "heal": healAction(it, a, line); break;

            // ---- movement -----------------------------------------------------------
            case "teleport": teleportAction(it, a, line); break;

            // ---- world/state ----------------------------------------------------------
            case "weather": case "setweather": r.setWeather(text(a, 0, "set weather to rain")); break;
            case "time": case "settime": r.setTime(text(a, 0, "set time to night")); break;
            case "setblock": case "place": case "placeblock": placeBlock(it, a, line); break;
            case "spawn": spawnAction(it, a, line); break;
            case "despawn": r.despawn(text(a, 0, "despawn zombies")); break;
            case "playsound": case "sound": r.playSound(target(it, a, 0), text(a, 1, "play sound")); break;
            case "particle": case "playparticle": r.playParticle(target(it, a, 0), text(a, 1, "play particle")); break;
            case "lightning": r.strikeLightningAt(target(it, a, 0)); break;
            case "ignite": case "setonfire": r.ignite(target(it, a, 0), (int) amount(it, a, 1, "ignite player for 5 seconds")); break;
            case "freeze": r.freeze(target(it, a, 0), 5); break;
            case "effect": case "giveeffect": giveEffect(it, a, line); break;
            case "apply": applyAction(it, a, line); break;
            case "strike": r.strikeLightningAt(target(it, a, 0)); break;
            case "smite": r.strikeLightningAt(target(it, a, 0)); break;
            case "explode": explodeAction(it, a, line); break;
            case "feed": feedAction(it, a, line); break;
            case "enchant": enchantAction(it, a, line); break;
            case "unenchant": r.unenchant(target(it, a, 0), text(a, 1, "unenchant")); break;

            // ---- doors / game -----------------------------------------------------------
            case "open": openAction(it, a, line); break;
            case "close": closeAction(it, a, line); break;
            case "win": r.winGame(target(it, a, 0)); break;
            case "lose": r.loseGame(target(it, a, 0)); break;

            // ---- economy ----------------------------------------------------------------
            case "pay": pay(it, a, line); break;
            case "charge": charge(it, a, line); break;
            case "deposit": coinMove(it, a, +1, line); break;
            case "withdraw": coinMove(it, a, -1, line); break;
            case "balance": case "check": balance(it, a, line); break;

            // ---- admin -----------------------------------------------------------------
            case "make": makeAction(it, a, line); break;
            case "op": r.setOperator(target(it, a, 0), true); break;
            case "deop": r.setOperator(target(it, a, 0), false); break;
            case "invisible": r.setInvisible(target(it, a, 0), !containsWord(a, "visible")); break;
            case "visible": r.setInvisible(target(it, a, 0), false); break;
            case "glowing": r.setGlowing(target(it, a, 0), true); break;
            case "gravity": {
                boolean on = !(containsWord(a, "off") || containsWord(a, "no"));
                r.setGravity(target(it, a, 0), on);
                break;
            }
            case "reset": r.resetPlayer(target(it, a, 0)); break;
            case "cancel": if (!a.isEmpty() && a.get(0).equalsIgnoreCase("event")) it.cancelEvent(); break;
            case "ban": r.ban(target(it, a, 0)); break;
            case "kick": r.kick(target(it, a, 0), text(a, 1, "kick player")); break;
            case "mute": r.setMute(target(it, a, 0), true); break;
            case "givepermission": case "permission": r.givePermission(target(it, a, 0), text(a, 1, "give permission")); break;
            case "removepermission": r.removePermission(target(it, a, 0), text(a, 1, "remove permission")); break;
            case "gamemode": r.setGamemode(target(it, a, 0), text(a, 1, "set gamemode to creative")); break;
            case "setfly": case "fly": r.setFly(target(it, a, 0), true); break;
            case "setwalkspeed": r.setWalkSpeed(target(it, a, 0), amount(it, a, 1, "set walk speed")); break;
            case "setmobhealth": r.setMobHealth(text(a, 0, "set mob health"), amount(it, a, 1, "set mob health")); break;
            case "setmobspeed": r.setMobSpeed(text(a, 0, "set mob speed"), amount(it, a, 1, "set mob speed")); break;
            case "setsneaking": r.setSneaking(target(it, a, 0), !containsWord(a, "off")); break;
            case "makesneak": r.setSneaking(target(it, a, 0), true); break;
            case "setsprinting": r.setSprinting(target(it, a, 0), !containsWord(a, "off")); break;
            case "makesprint": r.setSprinting(target(it, a, 0), true); break;
            case "vanish": r.setHideFrom(target(it, a, 0), McRuntime.ALL); break;
            case "hide": {
                int from = indexOfIgnoreCase(a, "from");
                String t = target(it, a, 0);
                String other = from >= 0 && from + 1 < a.size() ? target(it, a, from + 1) : McRuntime.ALL;
                r.setHideFrom(t, other);
                break;
            }
            case "unvanish": case "reveal": r.setShowTo(target(it, a, 0), McRuntime.ALL); break;
            case "show": {
                int to = indexOfIgnoreCase(a, "to");
                String t = target(it, a, 0);
                String other = to >= 0 && to + 1 < a.size() ? target(it, a, to + 1) : McRuntime.ALL;
                r.setShowTo(t, other);
                break;
            }
            case "setarmor": {
                double[] arm = numbersIn(a);
                r.setArmor(target(it, a, 0), arm.length > 0 ? arm[0] : 0);
                break;
            }
            case "setarmorpoints": {
                double[] armp = numbersIn(a);
                r.setArmor(target(it, a, 0), armp.length > 0 ? armp[0] : 0);
                break;
            }
            case "setabsorption": {
                double[] abs = numbersIn(a);
                r.setAbsorption(target(it, a, 0), abs.length > 0 ? abs[0] : 0);
                break;
            }
            case "cancelfalldamage": case "setfallprotection": {
                boolean on = !(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false"));
                r.cancelFallDamage(target(it, a, 0), on);
                break;
            }
            case "setinvincible": r.setInvincible(target(it, a, 0), !(containsWord(a, "off") || containsWord(a, "no"))); break;
            case "setcooldown": cooldownAction(it, a, r, line); break;

            // ---- inventory & slots -----------------------------------------------------
            case "swap": case "swaphands": r.swapHands(focusName(it)); break;
            case "setslot": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                int slot = 1;
                double[] sn = numbersIn(a.subList(0, to >= 0 ? to : a.size()));
                if (sn.length > 0) slot = (int) sn[0];
                String owner = ownerOf(it, a, of);
                String item = to >= 0 && to + 1 < a.size() ? VariableStore.join(a.subList(to + 1, a.size())) : "";
                r.setSlot(owner, slot, item.isEmpty() ? "diamond" : item);
                break;
            }
            case "setitemamount": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                int amount = 1;
                String item = "";
                if (to >= 0 && to + 1 < a.size()) {
                    List<String> rhs = a.subList(to + 1, a.size());
                    double[] an = numbersIn(rhs);
                    if (an.length > 0) { amount = (int) an[0]; item = VariableStore.join(rhs).replaceFirst("^\\d+(\\s+x\\d+)?\\s*", "").trim(); }
                    else item = VariableStore.join(rhs);
                }
                r.setItemAmount(owner, item.isEmpty() ? "item" : item, amount);
                break;
            }
            case "setunbreakable": {
                int of = indexOfIgnoreCase(a, "of");
                String owner = ownerOf(it, a, of);
                boolean on = !(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false"));
                String held = r.holdingItem(owner);
                r.setItemUnbreakable(owner, held.isEmpty() ? "item" : held, on);
                break;
            }
            case "setskullowner": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                String item = "";
                int end = to >= 0 ? to : a.size();
                if (of >= 0 && of + 2 < end) item = VariableStore.join(a.subList(of + 2, end)).trim();
                String skullFor = to >= 0 && to + 1 < a.size() ? interpText(it, a.subList(to + 1, a.size())) : "player";
                if (item.isEmpty()) item = r.holdingItem(owner);
                r.setSkullOwner(owner, item.isEmpty() ? "player head" : item, skullFor);
                break;
            }
            case "put": {
                // put diamond in slot 3 of player
                int in = indexOfIgnoreCase(a, "in");
                int of = indexOfIgnoreCase(a, "of");
                int slotIdx = indexOfIgnoreCase(a, "slot");
                String owner = ownerOf(it, a, of);
                int slot = 1;
                if (slotIdx >= 0 && slotIdx + 1 < a.size()) {
                    double[] psn = numbersIn(a.subList(slotIdx + 1, Math.min(slotIdx + 3, a.size())));
                    if (psn.length > 0) slot = (int) psn[0];
                }
                int end = in >= 0 ? in : a.size();
                String item = VariableStore.join(a.subList(0, end)).trim().replaceFirst("^\\d+(\\s+x\\d+)?\\s+", "");
                r.setSlot(owner, slot, item.isEmpty() ? "diamond" : item);
                break;
            }

            // ---- player tuning -----------------------------------------------------------
            case "setattackspeed": case "setsaturation": case "setair": {
                int of = indexOfIgnoreCase(a, "of");
                String owner = ownerOf(it, a, of);
                double[] vals = numbersIn(a);
                double val = vals.length > 0 ? vals[0] : amount(it, a, 1, "set attack speed to 12");
                if (verb.equals("setair")) r.setAir(owner, (int) val);
                else if (verb.equals("setsaturation")) r.setSaturation(owner, val);
                else r.setAttackSpeed(owner, val);
                break;
            }
            case "setflying": r.setFlying(ownerOf(it, a, indexOfIgnoreCase(a, "of")), !(containsWord(a, "off") || containsWord(a, "no"))); break;
            case "setgliding": r.setGliding(ownerOf(it, a, indexOfIgnoreCase(a, "of")), !(containsWord(a, "off") || containsWord(a, "no"))); break;
            case "setarmorslot": {
                // set armor slot of player to diamond helmet on head
                int on = indexOfIgnoreCase(a, "on");
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String piece = on >= 0 && on + 1 < a.size() ? a.get(on + 1) : "chest";
                String owner = ownerOf(it, a, of);
                int itemFrom = to >= 0 ? to + 1 : (of >= 0 ? of + 2 : 1);
                int itemTo = on >= 0 ? on : a.size();
                String item = itemFrom > 0 && itemFrom < itemTo ? VariableStore.join(a.subList(itemFrom, itemTo)).trim() : "diamond chestplate";
                r.setArmorSlot(owner, item, piece);
                break;
            }
            case "setdisplayname": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                String name = to >= 0 && to + 1 < a.size() ? interpText(it, a.subList(to + 1, a.size())) : text(a, 1, "set display name");
                r.setDisplayName(owner, name);
                break;
            }
            case "setlistname": case "setnametag": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                String name = to >= 0 && to + 1 < a.size() ? interpText(it, a.subList(to + 1, a.size())) : text(a, 1, "set list name");
                r.setPlayerListName(owner, name);
                break;
            }
            case "setglowcolor": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                String color = to >= 0 && to + 1 < a.size() ? VariableStore.join(a.subList(to + 1, a.size())) : "yellow";
                r.setGlowColor(owner, color);
                break;
            }
            case "settablistheader": {
                int of = indexOfIgnoreCase(a, "of");
                int to = indexOfIgnoreCase(a, "to");
                String owner = ownerOf(it, a, of);
                String header = "";
                String footer = "";
                if (to >= 0 && to + 1 < a.size()) {
                    List<String> tail = new ArrayList<>(a.subList(to + 1, a.size()));
                    int andIdx = indexOfIgnoreCase(tail, "and");
                    if (andIdx >= 0) {
                        header = interpText(it, tail.subList(0, andIdx));
                        footer = interpText(it, tail.subList(andIdx + 1, tail.size()));
                    } else {
                        header = interpText(it, tail);
                    }
                }
                r.setTabListHeaderFooter(owner, header, footer);
                break;
            }
            case "setrespawn": case "setbedspawn": r.setRespawnPoint(ownerOf(it, a, indexOfIgnoreCase(a, "of"))); break;
            case "launch": r.launch(focusName(it), amount(it, a, 1, "launch player 5")); break;
            case "cleareffects": case "removealleffects": r.removeAllEffects(ownerOf(it, a, indexOfIgnoreCase(a, "of"))); break;
            case "setfoodlevel": {
                int of = indexOfIgnoreCase(a, "of");
                String owner = ownerOf(it, a, of);
                double[] fn = numbersIn(a);
                r.setFood(owner, fn.length > 0 ? fn[0] : amount(it, a, 1, "set food level to 20"));
                break;
            }
            case "fullheal": r.healToFull(ownerOf(it, a, indexOfIgnoreCase(a, "of"))); break;
            case "unfreeze": r.freeze(ownerOf(it, a, indexOfIgnoreCase(a, "of")), 0); break;
            case "feedfull": r.feed(ownerOf(it, a, indexOfIgnoreCase(a, "of")), 20); break;

            // ---- broadcasts --------------------------------------------------------------
            case "broadcasttitle": r.title(McRuntime.ALL, interpText(it, a), ""); break;
            case "broadcastactionbar": r.actionbar(McRuntime.ALL, interpText(it, a)); break;
            case "broadcasttoast": r.toast(McRuntime.ALL, interpText(it, a)); break;
            case "broadcastsound": case "soundall": r.playSound(McRuntime.ALL, text(a, 0, "play sound")); break;
            case "stopsounds": r.stopAllSounds(ownerOf(it, a, indexOfIgnoreCase(a, "of"))); break;
            case "musicdisc": {
                int forIdx = indexOfIgnoreCase(a, "for");
                int of = indexOfIgnoreCase(a, "of");
                String owner = (forIdx >= 0 || of >= 0) ? ownerOf(it, a, forIdx >= 0 ? forIdx : of) : focusName(it);
                int cut = Math.max(forIdx, of);
                String disc = cut >= 0 ? VariableStore.join(a.subList(0, cut)).trim() : VariableStore.join(a);
                r.playMusicDisc(owner, disc.isEmpty() ? "cat" : disc);
                break;
            }

            // ---- world effects (coordinate based) ----------------------------------------
            case "dropexperience": {
                double[] ex = numbersIn(a);
                Location at = ex.length >= 3 ? Location.at(worldOf(it), ex[0], ex[1], ex[2]) : r.locationOf(it.focus());
                int amt = ex.length >= 4 ? (int) ex[3] : (ex.length >= 1 ? (int) ex[0] : 10);
                r.dropExperience(at, amt);
                break;
            }
            case "dropat": case "dropitemat": {
                double[] da = numbersIn(a);
                Location at = da.length >= 3 ? Location.at(worldOf(it), da[0], da[1], da[2]) : r.locationOf(it.focus());
                String item = VariableStore.join(a).replaceFirst("^\\d+(\\s+\\d+){2}(\\s+\\d+)?\\s*", "");
                r.dropItemAt(at, item.isEmpty() ? "diamond" : item, da.length >= 4 ? (int) da[3] : 1);
                break;
            }
            case "lightningat": {
                double[] la = numbersIn(a);
                if (la.length >= 3) r.lightningAt(Location.at(worldOf(it), la[0], la[1], la[2]));
                else r.strikeLightningAt(ownerOf(it, a, -1));
                break;
            }
            case "playsoundat": {
                double[] sa = numbersIn(a);
                Location at = sa.length >= 3 ? Location.at(worldOf(it), sa[0], sa[1], sa[2]) : r.locationOf(it.focus());
                String sound = VariableStore.join(a).replaceFirst("^\\d+(\\s+\\d+){2}\\s*", "");
                r.playSoundAt(at, sound);
                break;
            }
            case "fill": case "fillregion": {
                double[] fa = numbersIn(a);
                int with = indexOfIgnoreCase(a, "with");
                String block = with >= 0 ? VariableStore.join(a.subList(with + 1, a.size())) : "stone";
                if (fa.length >= 6) {
                    r.fillRegion(Location.at(worldOf(it), fa[0], fa[1], fa[2]),
                            Location.at(worldOf(it), fa[3], fa[4], fa[5]), block);
                } else {
                    Location here = r.locationOf(it.focus());
                    r.fillRegion(here, here, block);
                }
                break;
            }
            case "randomitem": {
                int to = indexOfIgnoreCase(a, "to");
                int forIdx = indexOfIgnoreCase(a, "for");
                r.giveRandomItem((to >= 0 || forIdx >= 0) ? ownerOf(it, a, to >= 0 ? to : forIdx) : focusName(it));
                break;
            }

            // ---- xp / levels -------------------------------------------------------------
            case "experience": case "xp": r.giveXp(target(it, a, 0), amount(it, a, 0, "give xp")); break;
            case "levels": r.giveLevels(target(it, a, 0), amount(it, a, 0, "give levels")); break;

            // ---- regions ------------------------------------------------------------------
            case "define": defineArea(it, a, line); break;

            // ---- scoreboards --------------------------------------------------------------
            case "setscore": scoreAction(it, a, r, false, false, line); break;
            case "addscore": scoreAction(it, a, r, true, false, line); break;
            case "removescore": scoreAction(it, a, r, false, true, line); break;
            case "createscoreboard": {
                String objective = a.isEmpty() ? "score" : a.get(0);
                String display = VariableStore.join(a.subList(1, a.size()));
                if (display.isEmpty()) display = objective;
                r.createScoreboard(objective, display);
                break;
            }
            case "setscoreboarddisplay": r.setScoreboardDisplay(text(a, 0, "set scoreboard display")); break;
            case "deletescoreboard": r.deleteScoreboard(text(a, 0, "delete scoreboard")); break;

            // ---- teams ----------------------------------------------------------------------
            case "createteam": r.createTeam(text(a, 0, "create team red")); break;
            case "addtoteam": teamMove(it, a, r, true, line); break;
            case "removefromteam": teamMove(it, a, r, false, line); break;
            case "setteamcolor": teamValue(it, a, r, "color", line); break;
            case "setteamprefix": teamValue(it, a, r, "prefix", line); break;
            case "setteamsuffix": teamValue(it, a, r, "suffix", line); break;

            // ---- boss bars --------------------------------------------------------------
            case "createbossbar": {
                String name = a.isEmpty() ? "boss" : a.get(0);
                String title = VariableStore.join(a.subList(1, a.size()));
                r.createBossBar(name, title.isEmpty() ? "Boss" : title);
                break;
            }
            case "setbossbar": {
                String j = VariableStore.join(a).toLowerCase();
                String name = a.isEmpty() ? "boss" : a.get(0);
                if (j.contains("progress")) {
                    double[] nums = numbersIn(a);
                    r.bossBarProgress(name, nums.length > 0 ? nums[nums.length - 1] : 0.5);
                    break;
                }
                if (j.contains("color")) { r.bossBarColor(name, textAfter(it, a, "to", "set boss bar color")); break; }
                if (j.contains("style")) { r.bossBarStyle(name, textAfter(it, a, "to", "set boss bar style")); break; }
                r.bossBarTitle(name);
                break;
            }
            case "setbossbarcolor": bossBarValue(it, a, r, "color", line); break;
            case "setbossbarstyle": bossBarValue(it, a, r, "style", line); break;
            case "setbossbarprogress": bossBarProgress(it, a, r, line); break;
            case "showbossbar": r.bossBarVisible(text(a, 0, "show boss bar"), true); break;
            case "hidebossbar": r.bossBarVisible(text(a, 0, "hide boss bar"), false); break;
            case "removebossbar": r.removeBossBar(text(a, 0, "remove boss bar")); break;

            // ---- world extras -------------------------------------------------------------
            case "setborder": r.setWorldBorder(amount(it, a, 0, "set world border 5000")); break;
            case "setdifficulty": r.setDifficulty(text(a, 0, "set difficulty peaceful")); break;
            case "setspawnpoint": {
                double[] nums = numbersIn(a);
                if (nums.length >= 3) r.setSpawnPoint(Location.at("world", nums[0], nums[1], nums[2]));
                else r.setSpawnPoint(r.locationOf(it.focus()));
                break;
            }
            case "setmoblimit": r.setMobLimit((int) amount(it, a, 0, "set mob limit 20")); break;
            case "setworldrule":
            case "setexplosiondamage": worldRule(it, a, r, line); break;
            case "setredstone": {
                int forIdx = indexOfIgnoreCase(a, "for");
                int toIdx = indexOfIgnoreCase(a, "to");
                String place;
                if (toIdx > 0) place = VariableStore.join(a.subList(0, toIdx));
                else if (forIdx >= 0 && forIdx + 1 < a.size()) place = VariableStore.join(a.subList(forIdx + 1, a.size()));
                else place = a.isEmpty() ? "signal" : a.get(0);
                place = place.replaceFirst("(?i)^signal\\s*(for|of)?\\s*", "").trim();
                boolean on = !(containsWord(a, "off") || containsWord(a, "false") || containsWord(a, "no"));
                if (containsWord(a, "on") || containsWord(a, "true")) on = true;
                r.setRedstone(place.isEmpty() ? "gate" : place, on);
                break;
            }
            case "setweatherduration": r.setWeatherDuration(amount(it, a, 0, "set weather duration 300")); break;
            case "setstorm": r.setStorm(!(containsWord(a, "off") || containsWord(a, "no"))); break;
            case "setthunder": r.setThunder(!(containsWord(a, "off") || containsWord(a, "no"))); break;
            case "settimespeed": r.setTimeSpeed(amount(it, a, 0, "set time speed 3")); break;
            case "setplayerlimit": case "setslots": r.setPlayerLimit((int) amount(it, a, 0, "set player limit 20")); break;
            case "spawnstructure": {
                int at = indexOfIgnoreCase(a, "at");
                String name = at >= 0 ? VariableStore.join(a.subList(0, at)) : text(a, 0, "spawn structure");
                String loc = at >= 0 ? VariableStore.join(a.subList(at + 1, a.size())) : "";
                r.spawnStructure(name, loc);
                break;
            }

            // ---- item & armor ---------------------------------------------------------------
            case "renameitem": renameItemVerb(it, a, line); break;
            case "setlore": loreItemVerb(it, a, line); break;
            case "setmodeldata": modelDataVerb(it, a, line); break;
            case "setitemflag": setItemFlag(it, a, r, line); break;
            case "givehelmet": r.giveArmor(target(it, a, 0), text(a, 1, "give helmet netherite_helmet")); break;
            case "givearmor": r.giveArmor(target(it, a, 0), text(a, 1, "give player full iron armor")); break;
            case "sethelditem": r.setHeldItem(target(it, a, 0), text(a, 1, "set held item diamond")); break;
            case "setoffhanditem": r.setOffhandItem(target(it, a, 0), text(a, 1, "set offhand item torch")); break;
            case "clearinventory": r.clearItem(McRuntime.ALL, "all"); break;

            // ---- guis ---------------------------------------------------------------------------
            case "openmenu": {
                int forIdx = indexOfIgnoreCase(a, "for");
                int toIdx = indexOfIgnoreCase(a, "to");
                int cut = -1;
                if (forIdx >= 0) cut = forIdx; else if (toIdx >= 0) cut = toIdx;
                String t = it.focus();
                String menuName = cut >= 0 ? VariableStore.join(a.subList(0, cut)) : text(a, 0, "open menu rewards");
                if (cut >= 0) t = target(it, a, cut + 1);
                it.openMenu(menuName, t);
                r.openMenu(t, menuName);
                break;
            }
            case "openanvil": r.openAnvil(target(it, a, 0)); break;
            case "openworkbench": r.openWorkbench(target(it, a, 0)); break;
            case "openshop": r.openShop(target(it, a, 0)); break;
            case "openinventory": r.openMenu(target(it, a, 0), "inventory"); break;

            // ---- projectiles --------------------------------------------------------------------
            case "throw": {
                List<String> p = new ArrayList<>(a);
                if (!p.isEmpty() && p.get(0).matches("(?i)(an?|the)")) p.remove(0);
                r.throwItem(it.focus(), p.isEmpty() ? "pearl" : VariableStore.join(p));
                break;
            }
            case "shoot": {
                if (containsWord(a, "firework") || containsWord(a, "rocket")) {
                    String color = null;
                    for (String c : FIREWORK_COLORS) if (containsWord(a, c)) color = c;
                    if (color != null) r.shootColoredFirework(it.focus(), color);
                    else r.shootFirework(it.focus());
                    break;
                }
                List<String> p = new ArrayList<>(a);
                if (!p.isEmpty() && p.get(0).matches("(?i)(an?|the)")) p.remove(0);
                String t = a.isEmpty() ? it.focus() : target(it, a, 0);
                r.shoot(t, p.isEmpty() ? "arrow" : VariableStore.join(p));
                break;
            }
            case "shootfirework": r.shootFirework(it.focus()); break;
            case "shootcoloredfirework": r.shootColoredFirework(it.focus(), text(a, 0, "shoot a red firework")); break;
            case "addkill": r.addKill(target(it, a, 0)); break;
            case "adddeath": r.addDeath(target(it, a, 0)); break;

            // ---- mob extras ---------------------------------------------------------------------
            case "setmobage": r.setMobAge(text(a, 0, "set mob age"), (int) amount(it, a, 1, "set mob age")); break;
            case "setmobsize": r.setMobSize(text(a, 0, "set mob size"), amount(it, a, 1, "set mob size")); break;
            case "setmobpitch": r.setMobPitch(text(a, 0, "set mob pitch"), amount(it, a, 1, "set mob pitch")); break;
            case "tamemob": r.tameMob(text(a, 0, "tame mob")); break;
            case "namemob": r.nameMob(text(a, 0, "name mob"), text(a, 1, "name mob to Rex")); break;
            case "makemob": r.spawn(text(a, 0, "make mob"), r.locationOf(it.focus()), 1); break;
            case "setmobai": r.setMobAi(mobName(a), !containsWord(a, "off")); break;
            case "setmobgravity": r.setMobGravity(mobName(a), !containsWord(a, "off")); break;
            case "setmobflying": r.setMobFlying(mobName(a), !containsWord(a, "off")); break;
            case "setmobbreeding": r.setMobBreeding(mobName(a), !containsWord(a, "off")); break;
            case "setmobdrop": {
                int to = indexOfIgnoreCase(a, "to");
                String mob = to >= 0 ? VariableStore.join(a.subList(0, to)) : mobName(a);
                String item = to >= 0 ? interpText(it, a.subList(to + 1, a.size())) : "";
                r.setMobCustomDrop(mob, item);
                break;
            }
            case "setmobfollow": {
                int to = indexOfIgnoreCase(a, "to");
                int follows = indexOfIgnoreCase(a, "follows");
                int cut = to >= 0 ? to : follows;
                String mob = cut >= 0 ? VariableStore.join(a.subList(0, cut)) : mobName(a);
                String who = cut >= 0 && cut + 1 < a.size() ? target(it, a, cut + 1) : it.focus();
                r.setMobFollow(mob, who);
                break;
            }
            case "setmobtarget": {
                int cut = indexOfIgnoreCase(a, "target");
                if (cut < 0) cut = indexOfIgnoreCase(a, "to");
                String mob = cut >= 0 ? VariableStore.join(a.subList(0, cut)) : mobName(a);
                String who = cut >= 0 && cut + 1 < a.size() ? target(it, a, cut + 1) : it.focus();
                r.setMobTarget(mob, who);
                break;
            }
            case "setmobnamevisible": r.setMobNameVisible(mobName(a), !containsWord(a, "off")); break;
            case "setmobpersistent": r.setMobPersistent(mobName(a), !containsWord(a, "off")); break;

            // ---- server extras -------------------------------------------------------------------
            case "sendtolobby": r.sendToServer(target(it, a, 0), "lobby"); break;
            case "sendtoserver": r.sendToServer(target(it, a, 0), text(a, 1, "send to server hub")); break;
            case "settablist": r.setTabList(target(it, a, 0), text(a, 1, "set tab list name")); break;
            case "setclickcommand": r.setClickCommand(target(it, a, 0), text(a, 1, "set click command")); break;
            case "setsigntext": r.setSignText(locationAt(it, a), text(a, 1, "set sign text")); break;
            case "setmute": r.setMute(target(it, a, 0), true); break;
            case "unmute": r.setMute(target(it, a, 0), false); break;
            case "revive": r.setGravity(target(it, a, 0), true); r.healToFull(target(it, a, 0)); break;
            case "unban": r.ban(target(it, a, 0)); break;

            // ---- flags & quests -------------------------------------------------------------------
            case "setflag": {
                int to = indexOfIgnoreCase(a, "to");
                String name = to >= 0 ? VariableStore.join(a.subList(0, to)) : text(a, 0, "set flag safe-zone to true");
                boolean on = to < 0 || !(containsWord(a.subList(to, a.size()), "off")
                        || containsWord(a.subList(to, a.size()), "false"));
                r.setFlag(name, on);
                break;
            }
            case "toggleflag": r.setFlag(text(a, 0, "toggle flag"), !r.getFlag(text(a, 0, "toggle flag"))); break;
            case "setquest": {
                int to = indexOfIgnoreCase(a, "to");
                String name = to >= 0 ? VariableStore.join(a.subList(0, to)) : text(a, 0, "set quest main to 3");
                double value = to >= 0 ? amount(it, a, to + 1, "set quest progress to 5") : 1;
                r.setQuest(name.trim().isEmpty() ? "main" : name.trim(), value);
                break;
            }
            case "completequest": r.completeQuest(text(a, 0, "complete quest")); break;

            // ---- game flow ---------------------------------------------------------------------------
            case "startgame": r.startGame(text(a, 0, "start game")); break;
            case "endgame": r.endGame(text(a, 0, "end game")); break;
            case "setround": r.setRound((int) amount(it, a, 0, "set round 2")); break;
            case "nextround": r.nextRound(); break;

            // ---- more phrasal verbs -------------------------------------------------------------
            case "breakblock": r.breakBlock(locationAt(it, a)); break;
            case "strikelightning": {
                int at = indexOfIgnoreCase(a, "at");
                String t = at >= 0 ? target(it, a, at + 1) : target(it, a, 0);
                r.strikeLightningAt(t);
                break;
            }
            case "setgamemode": {
                int to = indexOfIgnoreCase(a, "to");
                String t = target(it, a, 0);
                String mode = to >= 0 ? VariableStore.join(a.subList(to + 1, a.size())) : text(a, 1, "set gamemode to creative");
                r.setGamemode(t, mode);
                break;
            }
            case "setinvulnerable": r.setInvulnerable(target(it, a, 0), true); break;
            case "setflyspeed": {
                int of = indexOfIgnoreCase(a, "of");
                String owner = ownerOf(it, a, of);
                double[] fv = numbersIn(a);
                r.setFlySpeed(owner, fv.length > 0 ? fv[0] : amount(it, a, 1, "set fly speed to 0.1"));
                break;
            }
            case "setmobhostility": r.setMobHostility(text(a, 0, "set mob hostility"), !containsWord(a, "false")); break;
            case "extinguish": r.ignite(target(it, a, 0), 0); break;
            case "setfrozen": r.freeze(target(it, a, 0), 999); break;
            case "removeeffect": r.removeEffect(target(it, a, 0), text(a, 1, "remove effect")); break;
            case "givexp": r.giveXp(target(it, a, 0), amount(it, a, 1, "give xp")); break;
            case "givelevels": r.giveLevels(target(it, a, 0), amount(it, a, 1, "give levels")); break;
            case "setitem": {
                int to = indexOfIgnoreCase(a, "to");
                if (to >= 0) r.setHeldItem(target(it, a, to + 1), text(a, 0, "set item"));
                else r.setHeldItem(it.focus(), text(a, 0, "set item"));
                break;
            }
            case "setitemflags": setItemFlag(it, a, r, line); break;
            case "healtofull": r.healToFull(target(it, a, 0)); break;
            case "subtitle": r.title(target(it, a, 0), "", text(a, 1, "subtitle")); break;
            case "sethealth": r.setHealth(target(it, a, 0), amount(it, a, 1, "set health")); break;
            case "setmaxhealth": r.setMaxHealth(target(it, a, 0), amount(it, a, 1, "set max health")); break;
            case "setfood": r.setFood(target(it, a, 0), amount(it, a, 1, "set food")); break;
            case "setlevel": r.setLevel(target(it, a, 0), amount(it, a, 1, "set level")); break;
            case "setinvisible": r.setInvisible(target(it, a, 0), true); break;
            case "setvisible": r.setInvisible(target(it, a, 0), false); break;
            case "setglowing": r.setGlowing(target(it, a, 0), true); break;
            case "setgravity": {
                boolean on = !(containsWord(a, "off") || containsWord(a, "no"));
                r.setGravity(target(it, a, 0), on);
                break;
            }
            case "setop": r.setOperator(target(it, a, 0), true); break;
            case "setmoney": setMoney(it, a, line); break;
            case "buy": buy(it, a, r, line); break;
            case "sell": addCoins(it, it.focus(), firstNumber(a, line)); break;
            case "setshopprice": {
                double price = firstNumber(a, line);
                r.setVillagerPrice("shop", text(a, 0, "set shop price"), price);
                break;
            }
            case "opendoor": r.openDoor(text(a, 0, "open door")); break;
            case "closedoor": r.closeDoor(text(a, 0, "close door")); break;
            case "opengate": r.openGate(text(a, 0, "open gate")); break;
            case "closegate": r.closeGate(text(a, 0, "close gate")); break;
            case "opentrapdoor": r.openDoor(text(a, 0, "open trapdoor")); break;
            case "closetrapdoor": r.closeDoor(text(a, 0, "close trapdoor")); break;
            case "makeplayer": makePlayer(it, a, r, line); break;
            case "executecommand": {
                int c = indexOfIgnoreCase(a, "command");
                String cmd = c >= 0 ? VariableStore.join(a.subList(c + 1, a.size())) : text(a, 0, "execute command /fly");
                r.executeCommand(it.focus(), cmd);
                break;
            }
            case "setvillagerprice": villagerPrice(it, a, r, line); break;
            case "setsidebartitle": r.setSidebarTitle(interpText(it, a)); break;
            case "setsidebarline": {
                double[] nums = numbersIn(a);
                int which = nums.length > 0 ? Math.max(0, (int) nums[0] - 1) : 0;
                int to = indexOfIgnoreCase(a, "to");
                String txt = to >= 0 ? interpText(it, a.subList(to + 1, a.size())) : interpText(it, a);
                r.setSidebarLine(which, txt);
                break;
            }

            // ---- verbs that arrive as single words but mean big library things --------
            case "wear": case "equip": {
                r.giveArmor(it.focus(), text(a, 0, "wear a diamond helmet"));
                break;
            }
            case "complete": case "finish": {
                int q = indexOfIgnoreCase(a, "quest");
                r.completeQuest(q >= 0 ? text(a, q + 1, "complete quest") : text(a, 0, "complete quest"));
                break;
            }
            case "send": case "transfer": {
                int server = indexOfIgnoreCase(a, "server");
                int hub = indexOfIgnoreCase(a, "hub");
                int lobby = indexOfIgnoreCase(a, "lobby");
                String dest = server >= 0 ? text(a, server + 1, "send to server")
                        : hub >= 0 ? "hub" : lobby >= 0 ? "lobby" : text(a, 0, "send to server");
                r.sendToServer(target(it, a, 0), dest);
                break;
            }

            // ---- depth batch: jail, homes, riding, repair, holograms, whitelist ----
            case "jail": r.jail(targetOrFocus(it, a)); break;
            case "unjail": case "free": case "release": r.unjail(targetOrFocus(it, a)); break;
            case "sethome": case "set home": r.setHome(targetOrFocus(it, a)); break;
            case "home": case "teleport home": case "goto home": r.teleportHome(targetOrFocus(it, a)); break;
            case "ride": case "mount": r.mount(targetOrFocus(it, a)); break;
            case "dismount": case "get off": r.dismount(targetOrFocus(it, a)); break;
            case "repair": {
                boolean all = containsWord(a, "all") || containsWord(a, "everything");
                // strip the quantifier so  repair all  repairs the focus player's gear, not "all"
                String t = targetOrFocus(it, all ? a.subList(0, 0) : a);
                r.repairItem(t, all);
                break;
            }
            case "hologram": case "spawnhologram": {
                // hologram player with text Welcome
                int with = indexOfIgnoreCase(a, "with");
                int of = indexOfIgnoreCase(a, "of");
                String t = targetOrFocus(it, a);
                String name = "hologram";
                if (of >= 0 && of + 1 < a.size()) name = text(a, of + 1, "hologram name");
                String txt = with >= 0 && with + 1 < a.size() ? interpText(it, a.subList(with + 1, a.size())) : interpText(it, a);
                r.spawnHologram(t, name, txt);
                break;
            }
            case "removehologram": {
                int of = indexOfIgnoreCase(a, "of");
                String t = targetOrFocus(it, a);
                String name = of >= 0 && of + 1 < a.size() ? text(a, of + 1, "hologram name") : "hologram";
                r.removeHologram(t, name);
                break;
            }
            case "whitelist": case "setwhitelisted":
                r.setWhitelisted(targetOrFocus(it, a), !(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false")));
                break;
            case "unwhitelist": r.setWhitelisted(targetOrFocus(it, a), false); break;

            default:
                // friendly "unknown action" error with a hint
                throw new VerbumError(line,
                        "I do not know the action  " + verb + "\n" +
                        "I know actions like  give, tell, kill, damage, heal, teleport, spawn, announce, set, add, remove, open, close, win, lose, ban, kick.\n" +
                        "Try writing the action you want in plain words.");
        }
    }

    // ------------------------------------------------------------- implementations

    // ---- natural-language dispatch for the big library ------------------------------
    // These run before the plain variable handlers so  set player's gamemode to creative
    // and  add Alex to team red  mean the runtime library, not a number.

    private static boolean setCapability(Interpreter it, List<String> a, McRuntime r, int line) {
        String joined = VariableStore.join(a);
        String j = joined.toLowerCase();
        int to = indexOfIgnoreCase(a, "to");
        String first = a.isEmpty() ? "" : a.get(0).toLowerCase();

        // When the value being assigned is itself a stored/live variable reference
        // (player's X, world's X, {X}, ...), this is a plain assignment like
        //   set x to world's border   - never hijack it as a library setter.
        if (to >= 0 && to + 1 < a.size()) {
            String val = VariableStore.join(a.subList(to + 1, a.size())).toLowerCase();
            if (val.contains("'s") || val.contains("::") || val.contains("{")
                    || val.startsWith("player ") || val.startsWith("world ")
                    || val.startsWith("global ") || val.startsWith("temp ")) {
                return false;
            }
        }

        // Only hijack plain  set  when the words clearly describe a library target,
        // never when they name a stored variable like  set player's score to player's coins.

        if (first.equals("score") || j.contains("scoreboard") || j.contains("score of ") || j.contains("score for ")) {
            scoreAction(it, a, r, false, false, line); return true;
        }
        if (first.equals("team") || j.contains(" team with")) {
            String team = to >= 0 ? text(a, to + 1, "set team to red") : text(a, 0, "set team to red");
            r.teamAdd(team, it.focus());
            return true;
        }
        if (first.equals("boss") || j.contains("boss bar")) {
            int bar = indexOfIgnoreCase(a, "bar");
            String name = bar >= 0 && bar + 1 < a.size() ? a.get(bar + 1) : "boss";
            if (j.contains("progress")) { bossBarProgress(it, a, r, line); return true; }
            if (j.contains("color")) { r.bossBarColor(name, textAfter(it, a, "to", "set boss bar color")); return true; }
            if (j.contains("style")) { r.bossBarStyle(name, textAfter(it, a, "to", "set boss bar style")); return true; }
            r.bossBarTitle(name);
            return true;
        }
        if (first.equals("flag")) {
            int f = indexOfIgnoreCase(a, "flag");
            String name = (to >= 0 && f >= 0 && f + 1 < to)
                    ? VariableStore.join(a.subList(f + 1, to))
                    : text(a, f + 1, "set flag safe-zone to true");
            name = name.replaceFirst("(?i)^(the\\s+|named\\s+|set\\s+)", "").trim();
            if (name.isEmpty()) name = "flag";
            boolean on = !(containsWord(a, "off") || containsWord(a, "false") || containsWord(a, "no"));
            r.setFlag(name, on);
            return true;
        }
        if (first.equals("quest") || j.startsWith("quest ")) {
            int q = indexOfIgnoreCase(a, "quest");
            double value = 1;
            if (to >= 0 && to + 1 < a.size()) {
                try { value = parseNum(a.get(to + 1), line); } catch (VerbumError ignore) {}
            }
            String quest = to >= 0 ? VariableStore.join(a.subList(q + 1, to)) : text(a, q + 1, "set quest main to 3");
            if (quest.trim().isEmpty()) quest = "main";
            if (j.contains("complete")) { r.completeQuest(quest); }
            else { r.setQuest(quest, value); }
            return true;
        }
        if (j.contains("gamemode") || j.contains("game mode")) {
            int gm = indexOfIgnoreCase(a, "gamemode") >= 0 ? indexOfIgnoreCase(a, "gamemode") : indexOfIgnoreCase(a, "mode");
            String mode = to >= 0 ? text(a, to + 1, "set gamemode to creative") : "survival";
            String t = it.focus();
            String head = a.isEmpty() ? "" : a.get(0).toLowerCase().replace("'", "");
            if (gm > 0 && !head.startsWith("player") && !head.startsWith("my") && !head.startsWith("the")) {
                t = target(it, a, 0).replaceAll("['’]s$", "");
            }
            r.setGamemode(t, mode);
            return true;
        }
        if (j.contains("difficulty")) {
            r.setDifficulty(to >= 0 ? text(a, to + 1, "set difficulty to hard") : text(a, 0, "set difficulty to hard"));
            return true;
        }
        if (j.contains("border")) {
            double radius = to >= 0 && to + 1 < a.size() ? parseNum(a.get(to + 1), line) : 1000;
            r.setWorldBorder(radius);
            return true;
        }
        if (j.contains("redstone")) {
            int rs = indexOfIgnoreCase(a, "redstone");
            String place = to >= 0 ? VariableStore.join(a.subList(rs + 1, to)) : text(a, rs + 1, "set redstone");
            place = place.replaceFirst("(?i)^signal\\s+(for|to)?\\s*", "").trim();
            boolean on = !(containsWord(a, "off") || containsWord(a, "false"));
            r.setRedstone(place.isEmpty() ? "gate" : place, on);
            return true;
        }
        if (j.contains("rule")) {
            int ru = indexOfIgnoreCase(a, "rule");
            String rule = to >= 0 ? VariableStore.join(a.subList(ru + 1, to)) : text(a, ru + 1, "set world rule");
            boolean on = !(containsWord(a, "off") || containsWord(a, "false"));
            if (containsWord(a, "on") || containsWord(a, "true")) on = true;
            r.setWorldRule(rule.trim().isEmpty() ? "keep-inventory" : rule.trim(), on);
            return true;
        }
        // body sensors set the state on the mock so  if player is swimming  can be read back
        if (j.contains("swimming") || j.contains("in water") || j.contains("gliding")
                || j.contains("falling") || j.contains("climbing")) {
            return true;
        }
        // plain body/life setters: only when the sentence clearly targets the player,
        // so  set world level to 5  stays a variable while  set player's health to 10  works
        boolean playerTalk = first.equals("player") || first.equals("player's") || first.equals("my")
                || first.equals("myself") || j.startsWith("player's") || j.startsWith("player");
        // Only treat as a body/life setter when the value is a real number, otherwise
        //  set player's hp to player's health  is a plain variable assignment.
        if (playerTalk && to >= 0 && to + 1 < a.size()) {
            double val = 0;
            boolean numeric;
            try { val = parseNum(a.get(to + 1), line); numeric = true; }
            catch (VerbumError ignore) { numeric = false; }
            if (numeric) {
                if (j.contains("health")) { r.setHealth(it.focus(), val); return true; }
                if (j.contains("food") || j.contains("hunger")) { r.setFood(it.focus(), val); return true; }
                if (j.contains("level") || j.contains("xp level")) { r.setLevel(it.focus(), val); return true; }
            }
        }
        return false;
    }

    private static boolean addCapability(Interpreter it, List<String> a, McRuntime r, int line) {
        String j = VariableStore.join(a).toLowerCase();
        if (j.contains("team")) { teamMove(it, a, r, true, line); return true; }
        if (j.contains("score")) { scoreAction(it, a, r, true, false, line); return true; }
        return false;
    }

    private static boolean removeCapability(Interpreter it, List<String> a, McRuntime r, int line) {
        String j = VariableStore.join(a).toLowerCase();
        if (j.contains("team")) { teamMove(it, a, r, false, line); return true; }
        if (j.contains("score")) { scoreAction(it, a, r, false, true, line); return true; }
        return false;
    }

    private static void setVariable(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line, "I need the word  to  in a  set  action.\nExample:  set player's coins to 100");
        List<String> targetWords = a.subList(0, to);
        List<String> valueWords = a.subList(to + 1, a.size());
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        it.store().set(scope, key, valueExpr(it, valueWords, line));
    }

    private static void addTo(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line, "I need the word  to  in  add.\nExample:  add 10 to player's coins");
        List<String> amountWords = a.subList(0, to);
        List<String> targetWords = a.subList(to + 1, a.size());
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        Object cur = it.store().get(scope, key);
        if (cur instanceof List<?>) {
            List<Object> l = new ArrayList<>((List<?>) cur);
            l.add(VariableStore.join(amountWords));
            it.store().set(scope, key, l);
            return;
        }
        double delta = new MathWords(it).numberOf(amountWords, line);
        applyDelta(it, targetWords, delta, line);
    }

    private static void removeFrom(Interpreter it, List<String> a, int line) {
        int from = indexOfIgnoreCase(a, "from");
        if (from < 0) throw new VerbumError(line, "I need the word  from  in  remove.\nExample:  remove 5 from player's coins");
        List<String> amountWords = a.subList(0, from);
        List<String> targetWords = a.subList(from + 1, a.size());
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        Object cur = it.store().get(scope, key);
        if (cur instanceof List<?>) {
            List<Object> l = new ArrayList<>((List<?>) cur);
            final String item = VariableStore.join(amountWords);
            l.removeIf(o -> o.toString().equalsIgnoreCase(item));
            it.store().set(scope, key, l);
            return;
        }
        double delta = -new MathWords(it).numberOf(amountWords, line);
        applyDelta(it, targetWords, delta, line);
    }

    private static void multiply(Interpreter it, List<String> a, int line) {
        int by = indexOfIgnoreCase(a, "by");
        List<String> t = by < 0 ? a : a.subList(0, by);
        List<String> m = by < 0 ? List.of("2") : a.subList(by + 1, a.size());
        applyOp(it, t, new MathWords(it).numberOf(m, line), '*', line);
    }

    private static void divide(Interpreter it, List<String> a, int line) {
        int by = indexOfIgnoreCase(a, "by");
        List<String> t = by < 0 ? a : a.subList(0, by);
        List<String> d = by < 0 ? List.of("2") : a.subList(by + 1, a.size());
        applyOp(it, t, new MathWords(it).numberOf(d, line), '/', line);
    }

    private static void incdec(Interpreter it, List<String> a, double sign, int line) {
        int by = indexOfIgnoreCase(a, "by");
        List<String> t = by < 0 ? a : a.subList(0, by);
        List<String> amt = by < 0 ? List.of("1") : a.subList(by + 1, a.size());
        applyDelta(it, t, sign * new MathWords(it).numberOf(amt, line), line);
    }

    private static void applyDelta(Interpreter it, List<String> targetWords, double delta, int line) {
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        double cur = it.store().has(scope, key) ? it.store().asNumber(it.store().get(scope, key), line) : 0;
        it.store().set(scope, key, cur + delta);
    }

    private static void applyOp(Interpreter it, List<String> targetWords, double operand, char op, int line) {
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        double cur = it.store().has(scope, key) ? it.store().asNumber(it.store().get(scope, key), line) : 0;
        double result = op == '*' ? cur * operand : (operand == 0 ? 0 : cur / operand);
        it.store().set(scope, key, result);
    }

    private static Object valueObject(Interpreter it, List<String> words, int line) {
        if (words.isEmpty()) return "";
        String joined = VariableStore.join(words);
        if (words.size() == 1) {
            String w = words.get(0);
            if (w.equalsIgnoreCase("true")) return Boolean.TRUE;
            if (w.equalsIgnoreCase("false")) return Boolean.FALSE;
            if (w.equalsIgnoreCase("nothing") || w.equalsIgnoreCase("none")) return null;
            try { return Double.parseDouble(w.replace(",", "")); } catch (NumberFormatException ignore) {}
        }
        // a variable reference, e.g.  set score to player's coins
        Object[] ref = VariableStore.resolve(words);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        if (it.store().has(scope, key)) return it.store().get(scope, key);
        Object live = liveValue(it, scope, key);
        if (live != null) return live;
        return joined;
    }

    /**
     * Skript-style live values:  player's health, player's y-coordinate, amount of
     * all players, the weather ... These are computed from the runtime when no
     * stored variable with that name exists. Returns null when unknown.
     */
    public static Object liveValue(Interpreter it, VariableStore.Scope scope, String key) {
        McRuntime r = it.runtime();
        String focus = it.focus();
        String k = key.toLowerCase();
        if (scope == VariableStore.Scope.PLAYER) {
            switch (k) {
                case "health": case "hp": case "hearts": return r.health(focus);
                case "max health": return r.maxHealth(focus);
                case "food": case "hunger": return r.food(focus);
                case "level": return (double) r.level(focus);
                case "experience": case "xp": case "total xp": case "total experience": return r.experience(focus);
                case "xp to level": case "experience to next level": case "xp needed":
                    return (double) r.xpToNextLevel(focus);
                case "xp percent": case "experience percent": case "xp bar": return r.xpPercent(focus);
                case "x": return r.coord(focus, 'x');
                case "y": case "altitude": return r.coord(focus, 'y');
                case "z": return r.coord(focus, 'z');
                case "gamemode": return r.gamemode(focus);
                case "biome": return r.biome(focus);
                case "dimension": return r.dimension(focus);
                case "world": case "world name": return r.worldName(focus);
                case "yaw": case "rotation": case "facing yaw": return r.yaw(focus);
                case "pitch": return r.pitch(focus);
                case "facing": case "direction": case "compass": case "facing direction": return r.facing(focus);
                case "saturation": case "saturation level": return r.saturation(focus);
                case "absorption": case "absorption hearts": return r.absorption(focus);
                case "air": case "breath": case "remaining air": case "oxygen": return (double) r.air(focus);
                case "max air": case "maximum air": return (double) r.maxAir(focus);
                case "fire ticks": case "burn time": case "burning ticks": return (double) r.fireTicks(focus);
                case "freeze ticks": case "frozen ticks": return (double) r.freezeTicks(focus);
                case "walk speed": return r.walkSpeed(focus);
                case "fly speed": return r.flySpeed(focus);
                case "ping": case "latency": case "ms": return (double) r.ping(focus);
                case "glowing": case "is glowing": return r.isGlowing(focus);
                case "invisible": case "is invisible": return r.isInvisible(focus);
                case "holding": case "held item": case "item in hand": return r.holdingItem(focus);
                case "held slot": case "hotbar slot": return (double) r.heldSlot(focus);
                case "empty slots": case "inventory space": case "free slots": return (double) r.emptySlots(focus);
                case "kills": return (double) r.killCount(focus);
                case "deaths": return (double) r.deathCount(focus);
                case "kill streak": case "streak": return (double) r.killStreak(focus);
                case "health percent": case "hp percent": return r.healthPercent(focus);
                case "armor": case "armor points": return r.armor(focus);
                case "sneaking": case "crouching": case "is sneaking": return r.isSneaking(focus);
                case "sprinting": case "is sprinting": return r.isSprinting(focus);
                case "flying": case "is flying": return r.isFlying(focus);
                case "on ground": case "grounded": case "is on ground": return r.isOnGround(focus);
                case "in water": case "is in water": return r.isInWater(focus);
                case "in lava": case "is in lava": return r.isInLava(focus);
                case "burning": case "is burning": return r.isBurning(focus);
                case "poisoned": case "is poisoned": return r.isPoisoned(focus);
                case "swimming": case "is swimming": return r.isSwimming(focus);
                case "gliding": case "is gliding": return r.isGliding(focus);
                case "falling": case "is falling": return r.isFalling(focus);
                case "climbing": case "is climbing": return r.isClimbing(focus);
                case "op": case "operator": case "is op": return r.isOp(focus);
                case "alive": case "is alive": return r.playerAlive(focus);
                case "muted": case "is muted": return r.isMuted(focus);
                case "weapon": case "held weapon": return r.weapon(focus);
                case "team": case "team name": return r.teamOf(focus);
                case "jailed": case "is jailed": case "in jail": return r.isJailed(focus);
                case "home": case "has home": case "has a home": return r.hasHome(focus);
                case "riding": case "is riding": case "in a vehicle": return r.isRiding(focus);
                case "whitelisted": case "is whitelisted": return r.isWhitelisted(focus);
                default: return null;
            }
        }
        if (scope == VariableStore.Scope.WORLD) {
            switch (k) {
                case "time": case "time of day": return r.isDay() ? "day" : "night";
                case "world time": case "time ticks": case "ticks": return (double) r.worldTime();
                case "weather": return r.isStorm() ? "thunder" : r.isRain() ? "rain" : "sunny";
                case "temperature": return "";
                case "difficulty": return r.difficulty();
                case "day count": case "days passed": case "days": return (double) r.dayCount();
                case "seed": case "world seed": return (double) r.worldSeed();
                case "border": case "world border": case "border size": return r.worldBorder();
                case "spawn x": case "spawnpoint x": return r.spawnPoint().x();
                case "spawn y": case "spawnpoint y": return r.spawnPoint().y();
                case "spawn z": case "spawnpoint z": return r.spawnPoint().z();
                case "spawn world": case "spawnpoint world": return r.spawnPoint().world();
                default: return null;
            }
        }
        if (scope == VariableStore.Scope.GLOBAL) {
            switch (k) {
                case "all players": case "online players": case "online player count":
                case "player count": case "players online":
                    return (double) r.onlinePlayers();
                case "max players": case "player limit": case "server slots": case "max slots":
                    return (double) r.maxPlayers();
                case "open slots": case "slots left": case "free player slots":
                    return (double) Math.max(0, r.maxPlayers() - r.onlinePlayers());
                case "tps": case "tick rate": case "server tps": case "server's tps":
                    return r.tps();
                default: return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------- expression math

    /**
     * Evaluates a  set  value: a bare variable, plain text, or an arithmetic
     * expression like  player's coins * 2 plus 1  using + - * / (with word
     * aliases plus / minus / times / divided by / multiplied by).
     */
    private static Object valueExpr(Interpreter it, List<String> words, int line) {
        // a function call:  set player's answer to call double with 5
        if (!words.isEmpty() && words.get(0).equalsIgnoreCase("call")) {
            return it.runFunctionCall(words, line);
        }
        List<Object> toks = tokenizeExpr(words);
        boolean arithmetic = false;
        for (Object t : toks) if (t instanceof Character) { arithmetic = true; break; }
        if (!arithmetic) {
            // a bare user-defined function:  set x to double 5  (no  call  needed)
            if (!words.isEmpty() && it.hasFunction(words.get(0))) {
                List<String> call = new ArrayList<>(words);
                call.add(0, "call");
                return it.runFunctionCall(call, line);
            }
            // math helpers:  set x to floor of 4.7  (no operators, so handle before plain text)
            String joined = VariableStore.join(words).toLowerCase();
            if (isMathHelper(joined)) {
                return operandValue(it, words, line);
            }
            // text and list helpers:  uppercase of X, join of {list::*} by , ,
            // random element of {list::*}, alphabetically sorted {list::*}, ...
            Object helper = textAndListValue(it, words, line);
            if (helper != null) return helper;
            // a list literal:  set player's list to diamond and emerald and gold
            if (containsWord(words, "and") && words.size() >= 3) {
                List<Object> list = new ArrayList<>();
                List<String> cur = new ArrayList<>();
                for (String w : words) {
                    if (w.equalsIgnoreCase("and")) {
                        if (!cur.isEmpty()) { list.add(VariableStore.join(cur)); cur.clear(); }
                    } else {
                        cur.add(w);
                    }
                }
                if (!cur.isEmpty()) list.add(VariableStore.join(cur));
                return list;
            }
            return valueObject(it, words, line);
        }

        List<Double> nums = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        for (Object t : toks) {
            if (t instanceof List) nums.add(operandValue(it, (List<String>) t, line));
            else ops.add((Character) t);
        }
        // first pass: * and /
        List<Double> n2 = new ArrayList<>();
        List<Character> o2 = new ArrayList<>();
        n2.add(nums.get(0));
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k);
            if (k + 1 >= nums.size()) throw new VerbumError(line, "I expected a number after that operator.\nExample:  set x to 5 plus 3");
            double r = nums.get(k + 1);
            if (op == '*') n2.set(n2.size() - 1, n2.get(n2.size() - 1) * r);
            else if (op == '/') n2.set(n2.size() - 1, n2.get(n2.size() - 1) / r);
            else { n2.add(r); o2.add(op); }
        }
        double acc = n2.get(0);
        for (int k = 0; k < o2.size(); k++) {
            acc = o2.get(k) == '+' ? acc + n2.get(k + 1) : acc - n2.get(k + 1);
        }
        return acc;
    }

    private static List<Object> tokenizeExpr(List<String> words) {
        List<Object> toks = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        int i = 0;
        while (i < words.size()) {
            String w = words.get(i);
            if (i + 1 < words.size()) {
                String two = (w + " " + words.get(i + 1)).toLowerCase();
                if (two.equals("multiplied by")) { flush(cur, toks); toks.add('*'); i += 2; continue; }
                if (two.equals("divided by")) { flush(cur, toks); toks.add('/'); i += 2; continue; }
                if (two.equals("added to")) { flush(cur, toks); toks.add('+'); i += 2; continue; }
            }
            switch (w) {
                case "+": flush(cur, toks); toks.add('+'); i++; continue;
                case "-": flush(cur, toks); toks.add('-'); i++; continue;
                case "*": flush(cur, toks); toks.add('*'); i++; continue;
                case "/": flush(cur, toks); toks.add('/'); i++; continue;
                case "plus": flush(cur, toks); toks.add('+'); i++; continue;
                case "minus": flush(cur, toks); toks.add('-'); i++; continue;
                case "times": flush(cur, toks); toks.add('*'); i++; continue;
                case "over": flush(cur, toks); toks.add('/'); i++; continue;
            }
            cur.add(w);
            i++;
        }
        flush(cur, toks);
        return toks;
    }

    private static void flush(List<String> cur, List<Object> toks) {
        if (!cur.isEmpty()) { toks.add(new ArrayList<>(cur)); cur.clear(); }
    }

    private static double operandValue(Interpreter it, List<String> phrase, int line) {
        String j = VariableStore.join(phrase).toLowerCase();
        // a function call inside arithmetic:  call double with 5 plus 1
        if (!phrase.isEmpty()) {
            if (phrase.get(0).equalsIgnoreCase("call")) {
                return it.store().asNumber(it.runFunctionCall(phrase, line), line);
            }
            if (it.hasFunction(phrase.get(0))) {
                List<String> call = new ArrayList<>(phrase);
                call.add(0, "call");
                return it.store().asNumber(it.runFunctionCall(call, line), line);
            }
        }
        if (j.startsWith("length of") || j.startsWith("number of") || j.startsWith("size of")
                || j.startsWith("amount of")) {
            int of = indexOfIgnoreCase(phrase, "of");
            List<String> target = of >= 0 ? phrase.subList(of + 1, phrase.size()) : phrase;
            Object[] ref = VariableStore.resolve(target);
            Object v = it.store().get((VariableStore.Scope) ref[0], (String) ref[1]);
            if (v == null) v = liveValue(it, (VariableStore.Scope) ref[0], (String) ref[1]);
            if (v instanceof List<?> l) return l.size();
            if (v instanceof Number n) return n.doubleValue();
            return v == null ? 0 : String.valueOf(v).length();
        }
        // math helpers: floor of, ceil of, round of, absolute value of, sqrt of, max/min of
        int ofIdx = indexOfIgnoreCase(phrase, "of");
        if (ofIdx >= 0) {
            List<String> target = phrase.subList(ofIdx + 1, phrase.size());
            MathWords mw = new MathWords(it);
            if (j.startsWith("floor of")) return Math.floor(mw.numberOf(target, line));
            if (j.startsWith("ceil of") || j.startsWith("ceiling of")) return Math.ceil(mw.numberOf(target, line));
            if (j.startsWith("round of")) return Math.rint(mw.numberOf(target, line));
            if (j.startsWith("square root of") || j.startsWith("sqrt of")) return Math.sqrt(mw.numberOf(target, line));
            if (j.startsWith("absolute value of") || j.startsWith("abs of")) return Math.abs(mw.numberOf(target, line));
            if (j.startsWith("max of")) {
                int and = indexOfIgnoreCase(target, "and");
                if (and > 0) {
                    double a = mw.numberOf(target.subList(0, and), line);
                    double b = mw.numberOf(target.subList(and + 1, target.size()), line);
                    return Math.max(a, b);
                }
            }
            if (j.startsWith("min of")) {
                int and = indexOfIgnoreCase(target, "and");
                if (and > 0) {
                    double a = mw.numberOf(target.subList(0, and), line);
                    double b = mw.numberOf(target.subList(and + 1, target.size()), line);
                    return Math.min(a, b);
                }
            }
        }
        return new MathWords(it).numberOf(phrase, line);
    }

    /** Public entry so  return  statements can evaluate their value words. */
    public static Object value(Interpreter it, List<String> words, int line) {
        return valueExpr(it, words, line);
    }

    private static boolean isMathHelper(String joined) {
        return joined.startsWith("floor of") || joined.startsWith("ceil of")
                || joined.startsWith("ceiling of") || joined.startsWith("round of")
                || joined.startsWith("absolute value of") || joined.startsWith("abs of")
                || joined.startsWith("square root of") || joined.startsWith("sqrt of")
                || joined.startsWith("max of") || joined.startsWith("min of");
    }

    // ---- cooldowns & item metadata ---------------------------------------------------

    /**
     * Skript-style text and list helpers used in  set  values:
     *   uppercase of / lowercase of / capitalize of / trim of  X
     *   join of {list::*} by ,   |   random element of {list::*}
     *   alphabetically sorted / shuffled / reversed  {list::*}
     *   indices of {list::*}   |   size of {list::*}
     * Returns null when the phrase is not a helper.
     */
    private static Object textAndListValue(Interpreter it, List<String> words, int line) {
        if (words.isEmpty()) return null;
        String joined = VariableStore.join(words).toLowerCase();

        // text cases
        if (joined.startsWith("uppercase of") || joined.startsWith("lowercase of")
                || joined.startsWith("capitalize of") || joined.startsWith("trim of")) {
            int of = indexOfIgnoreCase(words, "of");
            List<String> tail = of >= 0 ? words.subList(of + 1, words.size()) : words;
            String text = helperText(it, tail, line);
            if (joined.startsWith("uppercase of")) return text.toUpperCase();
            if (joined.startsWith("lowercase of")) return text.toLowerCase();
            if (joined.startsWith("capitalize of")) return text.isEmpty() ? text
                    : Character.toUpperCase(text.charAt(0)) + text.substring(1);
            return text.trim();
        }

        // size of {list::*} / number of all players / length of a value
        if (joined.startsWith("size of") || joined.startsWith("number of")
                || joined.startsWith("length of") || joined.startsWith("amount of")) {
            int of = indexOfIgnoreCase(words, "of");
            Object v = resolved(it, words.subList(of + 1, words.size()));
            if (v instanceof List<?> l) return (double) l.size();
            if (v instanceof Number n) return n.doubleValue();
            return v == null ? 0.0 : (double) String.valueOf(v).length();
        }

        if (joined.startsWith("join of")) {
            int sepIdx = indexOfIgnoreCase(words, "by");
            if (sepIdx < 0) sepIdx = indexOfIgnoreCase(words, "with");
            int ofIdx = indexOfIgnoreCase(words, "of");
            List<String> target = sepIdx > 1 && ofIdx >= 0
                    ? words.subList(ofIdx + 1, sepIdx)
                    : words.subList(1, words.size());
            String sep = sepIdx > 1 ? VariableStore.join(words.subList(sepIdx + 1, words.size())) : " ";
            Object v = resolved(it, target);
            if (v instanceof List<?> l) return String.join(sep, l.stream().map(String::valueOf).toList());
            return VariableStore.asText(v);
        }

        if (joined.startsWith("random element of")) {
            int of = indexOfIgnoreCase(words, "of");
            Object v = resolved(it, words.subList(of + 1, words.size()));
            if (v instanceof List<?> l && !l.isEmpty()) {
                return l.get(ThreadLocalRandom.current().nextInt(l.size()));
            }
            return "";
        }

        // first of the list / last of the list  ->  a single element
        if (joined.startsWith("first of the list") || joined.startsWith("last of the list")) {
            int listIdx = indexOfIgnoreCase(words, "list");
            List<String> tail = listIdx >= 0 ? words.subList(listIdx + 1, words.size())
                    : words.subList(Math.max(1, indexOfIgnoreCase(words, "of") + 1), words.size());
            Object v = resolved(it, tail);
            if (v instanceof List<?> l && !l.isEmpty()) {
                return joined.startsWith("first of the list") ? l.get(0) : l.get(l.size() - 1);
            }
            return "";
        }

        if (joined.startsWith("alphabetically sorted") || joined.startsWith("shuffled")
                || joined.startsWith("reversed")) {
            int skip = joined.startsWith("alphabetically sorted") ? 2 : 1;
            Object v = resolved(it, words.subList(skip, words.size()));
            if (!(v instanceof List<?> l)) return v;
            List<Object> out = new ArrayList<>(l);
            if (joined.startsWith("alphabetically sorted")) {
                out.sort((a, b) -> String.valueOf(a).compareToIgnoreCase(String.valueOf(b)));
            } else if (joined.startsWith("shuffled")) {
                Collections.shuffle(out);
            } else if (joined.startsWith("reversed")) {
                Collections.reverse(out);
            }
            return out;
        }

        if (joined.startsWith("indices of")) {
            int of = indexOfIgnoreCase(words, "of");
            Object v = resolved(it, words.subList(of + 1, words.size()));
            List<Object> out = new ArrayList<>();
            if (v instanceof List<?> l) {
                for (int i = 1; i <= l.size(); i++) out.add(String.valueOf(i));
            } else if (v instanceof Map<?, ?> m) {
                out.addAll(m.keySet());
            }
            return out;
        }

        return null;
    }

    /** Reads a stored variable, or a live value (health, all players ...). */
    private static Object resolved(Interpreter it, List<String> target) {
        target = VariableStore.stripBraces(new ArrayList<>(target));
        Object[] ref = VariableStore.resolve(target);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        Object v = it.store().get(scope, key);
        if (v == null) v = liveValue(it, scope, key);
        return v == null ? List.of() : v;
    }

    /**
     * A helper tail as literal text: a stored or live value if the tail names
     * one, otherwise the words joined verbatim. Safe against reserved words
     * like  world  (the global scope keyword) that resolve() would object to.
     */
    private static String helperText(Interpreter it, List<String> tail, int line) {
        try { return VariableStore.asText(value(it, tail, line)); }
        catch (VerbumError e) { return VariableStore.join(tail); }
    }

    /** clear {list::*}  -> empty list;  delete {list::*} / clear {list::1}  -> remove the entry. */
    private static void clearVar(Interpreter it, String verb, List<String> a, int line) {
        List<String> target = new ArrayList<>(a);
        if (!target.isEmpty() && target.get(0).equalsIgnoreCase("variable")) {
            target = new ArrayList<>(target.subList(1, target.size()));
        }
        Object[] ref = VariableStore.resolve(target);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        if (key.endsWith("::*") && verb.equalsIgnoreCase("clear")) it.store().set(scope, key, new ArrayList<>());
        else it.store().removeVar(scope, key);
    }

    private static void cooldownAction(Interpreter it, List<String> a, McRuntime r, int line) {
        double[] nums = numbersIn(a);
        double secs = nums.length > 0 ? nums[nums.length - 1] : 5;
        int forIdx = indexOfIgnoreCase(a, "for");
        int on = indexOfIgnoreCase(a, "on");
        String t = it.focus();
        String action = "use";
        if (on >= 0) {
            t = target(it, a, on + 1);
            action = VariableStore.join(a.subList(0, on));
        } else if (forIdx >= 0) {
            t = target(it, a, 0);
            List<String> head = a.subList(1, forIdx);
            action = head.isEmpty() ? "use" : VariableStore.join(head);
        } else if (a.size() >= 2) {
            t = target(it, a, 0);
            action = a.get(1);
        }
        if (action.trim().isEmpty()) action = "use";
        r.setCooldown(t, action, secs);
    }

    private static void cooldown(Interpreter it, List<String> a, int line) {
        String secsText = a.isEmpty() ? "5" : a.get(0).replace(",", "");
        double secs;
        try {
            secs = Double.parseDouble(secsText);
        } catch (NumberFormatException e) {
            throw new VerbumError(line,
                    "I need a number of seconds after  cooldown.\nExample:  cooldown 5 seconds");
        }
        String key = "cd:" + it.focus() + ":" + it.currentScriptName();
        if (it.cooldownRemaining(key) > 0) it.stopOnCooldown();
        it.putCooldown(key, (long) (secs * 1000));
    }

    /** rename player's sword to Epic Sword */
    private static void renameItem(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line,
                "I need the word  to  in  rename.\nExample:  rename player's sword to Epic Sword");
        String t = possessiveTarget(it, a);
        String item = VariableStore.join(a.subList(1, to));
        String name = VariableStore.join(a.subList(to + 1, a.size()));
        it.runtime().renameItem(t, item, name);
    }

    /** lore player's sword to Shiny and Sharp */
    private static void loreItem(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line,
                "I need the word  to  in  lore.\nExample:  lore player's sword to Shiny and Sharp");
        String t = possessiveTarget(it, a);
        String item = VariableStore.join(a.subList(1, to));
        String lore = VariableStore.join(a.subList(to + 1, a.size()));
        it.runtime().setLore(t, item, lore);
    }

    /** modeldata player's sword to 100 */
    private static void modelData(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line,
                "I need the word  to  in  modeldata.\nExample:  modeldata player's sword to 100");
        String t = possessiveTarget(it, a);
        String item = VariableStore.join(a.subList(1, to));
        int data = a.size() > to + 1 ? (int) Double.parseDouble(a.get(to + 1).replace(",", "")) : 0;
        it.runtime().setCustomModelData(t, item, data);
    }

    /** "player's sword" -> the focus player; "Alex's sword" -> Alex; otherwise first word. */
    private static String possessiveTarget(Interpreter it, List<String> a) {
        if (a.isEmpty()) return it.focus();
        String first = a.get(0).toLowerCase();
        if (first.endsWith("'s")) {
            String owner = first.substring(0, first.length() - 2);
            if (owner.equalsIgnoreCase("player")) return it.focus();
            return owner;
        }
        return target(it, a, 0);
    }

    private static void saveData(Interpreter it, List<String> a, int line) {
        // save player's coins to database
        int to = indexOfIgnoreCase(a, "to");
        List<String> targetWords = to < 0 ? a : a.subList(0, to);
        Object[] ref = VariableStore.resolve(targetWords);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        if (it.store().has(scope, key)) {
            String persistKey = key + ":" + scope + ":" + it.focus();
            it.runtime().savePersistent(persistKey, it.store().get(scope, key));
        }
    }

    private static void loadData(Interpreter it, List<String> a, int line) {
        Object[] ref = VariableStore.resolve(a);
        VariableStore.Scope scope = (VariableStore.Scope) ref[0];
        String key = (String) ref[1];
        String persistKey = key + ":" + scope + ":" + it.focus();
        Object v = it.runtime().loadPersistent(persistKey);
        if (v != null) it.store().set(scope, key, v);
    }

    // ---- give/take -----------------------------------------------------------

    private static void giveAction(Interpreter it, List<String> a, int line) {
        if (a.isEmpty()) throw new VerbumError(line, "I need to know what to give and who to give it to.\nExample:  give player 5 diamonds");
        int to = indexOfIgnoreCase(a, "to");
        if (to >= 0) {
            // give 5 diamonds to player
            String targetName = target(it, a, to + 1);
            List<String> itemWords = a.subList(0, to);
            double count = 1;
            if (!itemWords.isEmpty() && isNumber(itemWords.get(0).replace(",", ""))) {
                count = Double.parseDouble(itemWords.get(0).replace(",", ""));
                itemWords = itemWords.subList(1, itemWords.size());
            }
            it.runtime().give(targetName, VariableStore.join(itemWords), count);
            return;
        }
        // give player 5 diamonds | give all players 1 dragon egg | give player diamond sword
        int i;
        String t;
        if (a.size() >= 2 && a.get(0).equalsIgnoreCase("all") && a.get(1).equalsIgnoreCase("players")) {
            t = McRuntime.ALL; i = 2;
        } else if (a.get(0).equalsIgnoreCase("all") || a.get(0).equalsIgnoreCase("everyone")
                || a.get(0).equalsIgnoreCase("everybody") || a.get(0).equalsIgnoreCase("players")) {
            t = McRuntime.ALL; i = 1;
        } else if (a.get(0).equalsIgnoreCase("player") || a.get(0).equalsIgnoreCase("me")) {
            t = it.focus().isEmpty() ? "Unknown" : it.focus(); i = 1;
        } else {
            t = a.get(0); i = 1;
        }
        double count = 1;
        if (i < a.size() && isNumber(a.get(i).replace(",", ""))) {
            count = Double.parseDouble(a.get(i).replace(",", ""));
            i++;
        }
        if (i >= a.size()) throw new VerbumError(line, "I need to know what item to give.\nExample:  give player 5 diamonds\nExample:  give player a diamond sword");
        int with = indexOfIgnoreCase(a, "with");
        String item = VariableStore.join(a.subList(i, with >= 0 ? with : a.size())).replaceFirst("(?i)^a ", "").replaceFirst("(?i)^an ", "");
        if (with >= 0) {
            double[] lvls = numbersIn(a.subList(with + 1, a.size()));
            int lvl = lvls.length > 0 ? (int) lvls[lvls.length - 1] : 1;
            String enchant = VariableStore.join(a.subList(with + 1, a.size()))
                    .replaceAll("(?i)\\s*(at|of|level)?\\s*\\d+\\s*$", "").trim();
            it.runtime().giveEnchanted(t, item, enchant.trim().isEmpty() ? "sharpness" : enchant, lvl);
        } else {
            it.runtime().give(t, item, count);
        }
    }

    /** Consumes the opening target of a give/take and returns the resolved name. */
    private static String consumeTarget(Interpreter it, List<String> a, int line) {
        if (a.isEmpty()) throw new VerbumError(line, "I need a target, like  give player 5 diamonds");
        String first = a.get(0);
        if (first.equalsIgnoreCase("all") || first.equalsIgnoreCase("everyone")
                || first.equalsIgnoreCase("everybody") || first.equalsIgnoreCase("players")) {
            return McRuntime.ALL;
        }
        if (first.equalsIgnoreCase("player") || first.equalsIgnoreCase("me")) {
            return it.focus().isEmpty() ? "Unknown" : it.focus();
        }
        return first;
    }

    private static void takeAction(Interpreter it, List<String> a, int line) {
        if (a.isEmpty()) throw new VerbumError(line, "I need to know what to take and who to take it from.\nExample:  take 5 diamonds from player");
        int from = indexOfIgnoreCase(a, "from");
        if (from >= 0) {
            String t = target(it, a, from + 1);
            List<String> itemWords = a.subList(0, from);
            double count = 1;
            if (!itemWords.isEmpty() && isNumber(itemWords.get(0).replace(",", ""))) {
                count = Double.parseDouble(itemWords.get(0).replace(",", ""));
                itemWords = itemWords.subList(1, itemWords.size());
            }
            it.runtime().take(t, VariableStore.join(itemWords), count);
            return;
        }
        String t;
        int i;
        if (a.size() >= 2 && a.get(0).equalsIgnoreCase("all") && a.get(1).equalsIgnoreCase("players")) {
            t = McRuntime.ALL; i = 2;
        } else if (a.get(0).equalsIgnoreCase("all") || a.get(0).equalsIgnoreCase("everyone")
                || a.get(0).equalsIgnoreCase("players")) {
            t = McRuntime.ALL; i = 1;
        } else if (a.get(0).equalsIgnoreCase("player") || a.get(0).equalsIgnoreCase("me")) {
            t = it.focus().isEmpty() ? "Unknown" : it.focus(); i = 1;
        } else {
            t = a.get(0); i = 1;
        }
        double count = 1;
        if (i < a.size() && isNumber(a.get(i).replace(",", ""))) { count = Double.parseDouble(a.get(i).replace(",", "")); i++; }
        if (i < a.size()) it.runtime().take(t, VariableStore.join(a.subList(i, a.size())), count);
    }

    private static void dropAction(Interpreter it, List<String> a, int line) {
        // drop 5 diamonds at player  /  drop player 5 diamonds  /  drop diamonds
        int at = indexOfIgnoreCase(a, "at");
        int to = indexOfIgnoreCase(a, "to");
        String t;
        List<String> itemWords;
        if (at >= 0) { t = target(it, a, at + 1); itemWords = a.subList(0, at); }
        else if (to >= 0) { t = target(it, a, to + 1); itemWords = a.subList(0, to); }
        else { t = it.focus().isEmpty() ? "Unknown" : it.focus(); itemWords = a; }
        double count = 1;
        if (!itemWords.isEmpty() && isNumber(itemWords.get(0).replace(",", ""))) {
            count = Double.parseDouble(itemWords.get(0).replace(",", ""));
            itemWords = itemWords.subList(1, itemWords.size());
        }
        if (itemWords.isEmpty()) throw new VerbumError(line, "I need to know what to drop.\nExample:  drop 5 diamonds at player");
        String item = VariableStore.join(itemWords).replaceFirst("(?i)^a ", "").replaceFirst("(?i)^an ", "");
        it.runtime().drop(t, item, count);
    }

    private static void placeBlock(Interpreter it, List<String> a, int line) {
        // set block stone at player  /  place diamond at 100 64 100
        int at = indexOfIgnoreCase(a, "at");
        List<String> blockWords, whereWords;
        if (at >= 0) { blockWords = a.subList(0, at); whereWords = a.subList(at + 1, a.size()); }
        else { blockWords = a; whereWords = List.of(); }
        double[] nums = numbersIn(whereWords);
        if (nums.length >= 3) {
            it.runtime().setBlock(VariableStore.join(blockWords), Location.at("world", nums[0], nums[1], nums[2]));
            return;
        }
        String t = whereWords.isEmpty() ? it.focus() : target(it, whereWords, 0);
        it.runtime().setBlock(VariableStore.join(blockWords), it.runtime().locationOf(t));
    }

    private static void applyAction(Interpreter it, List<String> a, int line) {
        // apply potion night vision to player for 30 seconds level 2
        int potionIdx = indexOfIgnoreCase(a, "potion");
        int effIdx = indexOfIgnoreCase(a, "effect");
        int toIdx = indexOfIgnoreCase(a, "to");
        String t = toIdx >= 0 ? target(it, a, toIdx + 1) : target(it, a, 0);
        int start = (potionIdx >= 0 ? potionIdx : effIdx) + 1;
        int end = toIdx >= 0 ? toIdx : a.size();
        List<String> effWords = a.subList(Math.max(0, start), Math.max(end, 0));
        String effect = VariableStore.join(effWords);
        int seconds = 30;
        int f = indexOfIgnoreCase(a, "for");
        if (f >= 0 && f + 1 < a.size()) seconds = (int) Double.parseDouble(a.get(f + 1).replace(",", ""));
        int lvl = 1;
        int lvlIdx = indexOfIgnoreCase(a, "level");
        if (lvlIdx >= 0 && lvlIdx + 1 < a.size()) lvl = (int) Double.parseDouble(a.get(lvlIdx + 1).replace(",", ""));
        it.runtime().giveEffect(t, effect, seconds, lvl);
    }

    private static void makeAction(Interpreter it, List<String> a, int line) {
        // make player execute command /warp spawn
        int exec = indexOfIgnoreCase(a, "command");
        if (exec < 0) throw new VerbumError(line, "I only know  make player execute command X\nExample:  make player execute command /warp");
        String cmd = VariableStore.join(a.subList(exec + 1, a.size()));
        String t = target(it, a, 0);
        it.runtime().executeCommand(t, cmd);
    }

    private static void explodeAction(Interpreter it, List<String> a, int line) {
        // explode player  /  explode player with power 5  /  explode player 5
        String t = target(it, a, 0);
        double power = 4;
        int with = indexOfIgnoreCase(a, "with");
        if (with >= 0 && with + 1 < a.size() && isNumber(a.get(with + 1).replace(",", ""))) {
            power = Double.parseDouble(a.get(with + 1).replace(",", ""));
        } else if (a.size() > 1 && isNumber(a.get(a.size() - 1).replace(",", ""))) {
            power = Double.parseDouble(a.get(a.size() - 1).replace(",", ""));
        }
        it.runtime().explode(t, power);
    }

    private static void feedAction(Interpreter it, List<String> a, int line) {
        // feed player  /  feed player to full  /  feed player 10
        String t = target(it, a, 0);
        double food = 20;
        int to = indexOfIgnoreCase(a, "to");
        if (to >= 0 && to + 1 < a.size() && isNumber(a.get(to + 1).replace(",", ""))) {
            food = Double.parseDouble(a.get(to + 1).replace(",", ""));
        } else if (a.size() > 1 && isNumber(a.get(a.size() - 1).replace(",", ""))) {
            food = Double.parseDouble(a.get(a.size() - 1).replace(",", ""));
        }
        it.runtime().feed(t, food);
    }

    private static void healAction(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to >= 0) {
            // heal player to full  /  heal to full
            String t = to > 0 ? target(it, a, 0) : it.focus();
            it.runtime().healToFull(t);
            return;
        }
        it.runtime().heal(target(it, a, 0), amount(it, a, 1, "heal player by 5"));
    }

    private static void giveEffect(Interpreter it, List<String> a, int line) {
        // give player night vision for 30 seconds
        String t = target(it, a, 0);
        int i = 1;
        String effect = VariableStore.join(a.subList(i, a.size()));
        int seconds = 30;
        if (indexOfIgnoreCase(a, i, "for") >= 0) {
            int f = indexOfIgnoreCase(a, i, "for");
            effect = VariableStore.join(a.subList(i, f));
            if (f + 1 < a.size()) seconds = (int) Double.parseDouble(a.get(f + 1).replace(",", ""));
        }
        it.runtime().giveEffect(t, effect, seconds, 1);
    }

    private static void enchantAction(Interpreter it, List<String> a, int line) {
        // enchant player's sword with sharpness level 3
        String t = target(it, a, 0);
        String item = containsWord(a, "sword") ? "sword" : containsWord(a, "bow") ? "bow" : "item";
        String enchant;
        int lvl = 1;
        int with = indexOfIgnoreCase(a, "with");
        if (with >= 0) {
            List<String> rest = a.subList(with + 1, a.size());
            enchant = rest.get(0);
            if (indexOfIgnoreCase(rest, 1, "level") >= 0) {
                int li = indexOfIgnoreCase(rest, 1, "level");
                if (li + 1 < rest.size()) lvl = (int) Double.parseDouble(rest.get(li + 1).replace(",", ""));
            }
        } else {
            enchant = a.size() > 1 ? a.get(1) : "sharpness";
        }
        it.runtime().enchant(t, enchant, item, lvl);
    }

    private static void teleportAction(Interpreter it, List<String> a, int line) {
        // teleport player to victory area
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) throw new VerbumError(line, "I need the word  to  in  teleport.\nExample:  teleport player to victory area");
        String t = target(it, a, 0);
        String place = VariableStore.join(a.subList(to + 1, a.size()));
        // exact coordinate form: teleport player to 100 64 100
        double[] nums = numbersIn(a.subList(to + 1, a.size()));
        if (nums.length >= 3) {
            it.runtime().teleportToCoords(t, Location.at("world", nums[0], nums[1], nums[2]));
            return;
        }
        it.runtime().teleportTo(t, place);
    }

    private static void spawnAction(Interpreter it, List<String> a, int line) {
        // spawn zombie  /  spawn 5 zombies  /  spawn boss
        int count = 1;
        int i = 0;
        if (!a.isEmpty() && isNumber(a.get(0).replace(",", ""))) {
            count = (int) Double.parseDouble(a.get(0).replace(",", ""));
            i = 1;
        }
        String mob = VariableStore.join(a.subList(i, a.size()));
        if (mob.isEmpty()) throw new VerbumError(line, "I need to know what to spawn.\nExample:  spawn zombie");
        Location at = it.runtime() instanceof dev.verbum.runtime.MockMcRuntime m
                ? m.player(it.focus()).loc : Location.at("world", 0, 64, 0);
        it.runtime().spawn(mob, at, count);
    }

    private static void openAction(Interpreter it, List<String> a, int line) {
        // open castle gate / open trading door / open rewards menu
        String w = VariableStore.join(a).toLowerCase();
        if (w.contains("menu")) { it.openMenu(noun(a, "menu"), it.focus()); return; }
        if (w.contains("door")) { it.runtime().openDoor(noun(a, "door")); return; }
        if (w.contains("gate")) { it.runtime().openGate(noun(a, "gate")); return; }
        throw new VerbumError(line, "I can only  open door ... ,  open gate ...  or  open menu ...");
    }

    private static void closeAction(Interpreter it, List<String> a, int line) {
        String w = VariableStore.join(a).toLowerCase();
        if (w.contains("door")) { it.runtime().closeDoor(noun(a, "door")); return; }
        if (w.contains("gate")) { it.runtime().closeGate(noun(a, "gate")); return; }
    }

    /** The noun that describes the thing, e.g. "open trading door" -> "trading". */
    private static String noun(List<String> a, String kind) {
        List<String> out = new ArrayList<>();
        for (String s : a) {
            if (s.equalsIgnoreCase(kind)) continue;
            if (s.equalsIgnoreCase("open") || s.equalsIgnoreCase("close")) continue;
            out.add(s);
        }
        return out.isEmpty() ? kind : VariableStore.join(out);
    }

    private static void titleAction(Interpreter it, List<String> a, int line) {
        // title player with Hello subtitle Welcome
        String t = target(it, a, 0);
        int with = indexOfIgnoreCase(a, "with");
        String title = "Title", subtitle = "";
        if (with >= 0) {
            title = VariableStore.join(a.subList(with + 1, a.size()));
        } else if (a.size() > 1) {
            title = VariableStore.join(a.subList(1, a.size()));
        }
        it.runtime().title(t, title, subtitle);
    }

    private static void defineArea(Interpreter it, List<String> a, int line) {
        // define area Spawn ... (optional 4 or 6 numbers as two corners)
        if (a.isEmpty()) throw new VerbumError(line, "I need a name after  define area.\nExample:  define area Spawn");
        if (!a.isEmpty() && a.get(0).equalsIgnoreCase("area")) a = new ArrayList<>(a.subList(1, a.size()));
        StringBuilder name = new StringBuilder();
        List<Double> nums = new ArrayList<>();
        boolean doneName = false;
        for (String s : a) {
            try {
                nums.add(Double.parseDouble(s.replace(",", "")));
                doneName = true;
            } catch (NumberFormatException e) {
                if (!doneName) {
                    if (name.length() > 0) name.append(' ');
                    name.append(s);
                }
            }
        }
        String areaName = name.length() == 0 ? a.get(0) : name.toString();
        double[] arr = nums.stream().mapToDouble(Double::doubleValue).toArray();
        Location c1, c2;
        dev.verbum.runtime.MockMcRuntime mock = it.runtime() instanceof dev.verbum.runtime.MockMcRuntime m ? m : null;
        Location p = mock != null ? mock.player(it.focus()).loc : Location.at("world", 0, 64, 0);
        if (arr.length >= 6) {
            c1 = Location.at("world", arr[0], arr[1], arr[2]);
            c2 = Location.at("world", arr[3], arr[4], arr[5]);
        } else if (arr.length >= 4) {
            c1 = Location.at("world", arr[0], 0, arr[1]);
            c2 = Location.at("world", arr[2], 256, arr[3]);
        } else {
            c1 = Location.at(p.world(), p.x() - 10, p.y() - 10, p.z() - 10);
            c2 = Location.at(p.world(), p.x() + 10, p.y() + 10, p.z() + 10);
        }
        it.runtime().defineArea(areaName, c1, c2);
    }

    // ------------------------------------------------------------- economy

    /** Reads the first number out of words (used by pay/charge). */
    private static double firstNumber(List<String> words, int line) {
        for (String w : words) {
            try { return Double.parseDouble(w.replace(",", "")); } catch (NumberFormatException ignore) {}
        }
        throw new VerbumError(line, "I expected an amount of money.\nExample:  pay player 50  or  charge player 20");
    }

    private static void addCoins(Interpreter it, String player, double amount) {
        var bag = it.store().playerVars(player.toLowerCase());
        double cur = bag.containsKey("coins") ? it.store().asNumber(bag.get("coins"), 0) : 0;
        bag.put("coins", cur + amount);
    }

    private static void pay(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to >= 0) {
            addCoins(it, target(it, a, to + 1), firstNumber(a.subList(0, to), line));
        } else if (a.size() >= 2) {
            String t = target(it, a, 0);
            addCoins(it, t, firstNumber(a.subList(1, a.size()), line));
        } else {
            addCoins(it, it.focus(), firstNumber(a, line));
        }
    }

    private static void charge(Interpreter it, List<String> a, int line) {
        int from = indexOfIgnoreCase(a, "from");
        if (from >= 0) {
            addCoins(it, target(it, a, from + 1), -firstNumber(a.subList(0, from), line));
        } else if (a.size() >= 2) {
            String t = target(it, a, 0);
            addCoins(it, t, -firstNumber(a.subList(1, a.size()), line));
        } else {
            addCoins(it, it.focus(), -firstNumber(a, line));
        }
    }

    private static void coinMove(Interpreter it, List<String> a, double sign, int line) {
        double amount = firstNumber(a, line);
        addCoins(it, it.focus(), sign * amount);
    }

    private static void balance(Interpreter it, List<String> a, int line) {
        var bag = it.store().playerVars(it.focus().toLowerCase());
        double cur = bag.containsKey("coins") ? it.store().asNumber(bag.get("coins"), 0) : 0;
        String shown = cur == Math.rint(cur) ? String.valueOf((long) cur) : String.valueOf(cur);
        it.runtime().tell(it.focus(), "You have " + shown + " coins");
    }



    /** Resolves the target word (player -> focus, all/players -> ALL). */
/** Target player when the phrase names one, else the current focus player. */
    static String targetOrFocus(Interpreter it, List<String> a) {
        if (a.isEmpty()) return focusName(it);
        return target(it, a, 0);
    }

    static String target(Interpreter it, List<String> a, int i) {
        if (i >= a.size()) return McRuntime.ALL;        String w = a.get(i);
        if (w.equalsIgnoreCase("player") || w.equalsIgnoreCase("me") || w.equalsIgnoreCase("myself")) {
            return it.focus().isEmpty() ? "Unknown" : it.focus();
        }
        if (w.equalsIgnoreCase("players") || w.equalsIgnoreCase("all")
                || w.equalsIgnoreCase("everyone") || w.equalsIgnoreCase("everybody")) {
            return McRuntime.ALL;
        }
        return w;
    }

    /** Focus player name ("Unknown" when none). */
    private static String focusName(Interpreter it) {
        return it.focus().isEmpty() ? "Unknown" : it.focus();
    }

    /** Owner after  of :  of player / of player's ...  -> focus,  of all  -> ALL, else literal. */
    private static String ownerOf(Interpreter it, List<String> a, int ofIdx) {
        if (ofIdx < 0 || ofIdx + 1 >= a.size()) return focusName(it);
        String w = a.get(ofIdx + 1).toLowerCase();
        if (w.equals("player") || w.equals("player's") || w.equals("me") || w.equals("myself")) return focusName(it);
        if (w.equals("players") || w.equals("players'") || w.equals("all") || w.equals("everyone")) return McRuntime.ALL;
        return a.get(ofIdx + 1);
    }

    private static String text(List<String> a, int from, String hint) {
        if (from >= a.size()) return "";
        return VariableStore.join(a.subList(from, a.size()));
    }

    /** World name of the focus player (for  drop experience at x y z  and friends). */
    private static String worldOf(Interpreter it) {
        return it.runtime().locationOf(it.focus()).world();
    }

    /** Message words with %variable% interpolation (used by tell, warn, toast, ...). */
    private static String message(Interpreter it, List<String> a, int from, String hint) {
        if (from >= a.size()) return "";
        return interpText(it, a.subList(from, a.size()));
    }

    private static String textAfter(Interpreter it, List<String> a, String marker, String hint) {
        int m = indexOfIgnoreCase(a, marker);
        if (m < 0 || m + 1 >= a.size()) return "";
        return interpText(it, a.subList(m + 1, a.size()));
    }

    /** Substitutes variables in a free message and drops a leading target word. */
    private static String interpText(Interpreter it, List<String> a) {
        String joined = VariableStore.join(a);
        StringBuilder out = new StringBuilder();
        int from = 0;
        while (true) {
            int s = joined.indexOf('%', from);
            if (s < 0) { out.append(joined.substring(from)); break; }
            int e = joined.indexOf('%', s + 1);
            if (e < 0) { out.append(joined.substring(from)); break; }
            out.append(joined, from, s);
            String ref = joined.substring(s + 1, e);
            Object[] r = VariableStore.resolve(List.of(ref.split(" ")));
            VariableStore.Scope scope = (VariableStore.Scope) r[0];
            String key = (String) r[1];
            Object v;
            if (scope != VariableStore.Scope.GLOBAL) {
                v = it.store().get(scope, key);
                if (v == null) v = liveValue(it, scope, key);
            } else {
                // unqualified ref: prefer global, then loop/temp vars, then the player
                if (it.store().has(VariableStore.Scope.GLOBAL, key)) v = it.store().get(VariableStore.Scope.GLOBAL, key);
                else if (it.store().has(VariableStore.Scope.TEMP, key)) v = it.store().get(VariableStore.Scope.TEMP, key);
                else if (it.store().has(VariableStore.Scope.PLAYER, key)) v = it.store().get(VariableStore.Scope.PLAYER, key);
                else v = liveValue(it, VariableStore.Scope.GLOBAL, key);
            }
            if (v == null) v = it.lookupParam(key);   // arg-1, lastarg, named params ...
            out.append(v == null ? "" : VariableStore.asText(v));
            from = e + 1;
        }
        return out.toString();
    }

    private static double amount(Interpreter it, List<String> a, int i, String hint) {
        if (i >= a.size()) throw new VerbumError("I expected a number.\nExample:  " + hint);
        return new MathWords(it).numberOf(List.of(a.get(i).replace(",", "")), 0);
    }

    private static boolean isNumber(String s) { try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; } }

    private static int indexOfIgnoreCase(List<String> a, String target) { return indexOfIgnoreCase(a, 0, target); }
    private static int indexOfIgnoreCase(List<String> a, int from, String target) {
        for (int i = from; i < a.size(); i++) if (a.get(i).equalsIgnoreCase(target)) return i;
        return -1;
    }

    private static boolean containsWord(List<String> a, String w) {
        for (String s : a) if (s.equalsIgnoreCase(w)) return true;
        return false;
    }

    private static double[] numbersIn(List<String> a) {
        List<Double> out = new ArrayList<>();
        for (String s : a) {
            try { out.add(Double.parseDouble(s.replace(",", ""))); } catch (NumberFormatException ignore) {}
        }
        return out.stream().mapToDouble(Double::doubleValue).toArray();
    }

    /** Mob name with trailing on/off/true/false flag words removed, e.g. "zombie king". */
    private static String mobName(List<String> a) {
        List<String> w = new ArrayList<>(a);
        while (!w.isEmpty() && w.get(w.size() - 1).matches("(?i)(on|off|true|false|yes|no|enabled|disabled)")) {
            w.remove(w.size() - 1);
        }
        if (w.isEmpty()) return "mob";
        return VariableStore.join(w);
    }

    private static final String[] FIREWORK_COLORS = {
            "red", "orange", "yellow", "green", "blue", "purple", "pink",
            "white", "black", "gold", "lime", "aqua", "silver", "cyan", "magenta"
    };

    // ------------------------------------------------------------- the giant library

    /** Friendly synonyms that all mean the same action. */
    private static String canonical(String verb) {
        String v = verb.toLowerCase();
        String c = SYNONYMS.get(v);
        return c != null ? c : v;
    }

    private static final java.util.Map<String, String> SYNONYMS = new java.util.HashMap<>();
    static {
        String[][] s = {
            {"kill", "kill"}, {"slay", "kill"}, {"execute", "kill"}, {"assassinate", "kill"}, {"murder", "kill"},
            {"damage", "damage"}, {"hurt", "damage"}, {"punch", "damage"}, {"wound", "damage"}, {"stab", "damage"},
            {"heal", "heal"}, {"cure", "heal"}, {"mend", "heal"}, {"restore", "heal"},
            {"ignite", "ignite"}, {"burn", "ignite"}, {"torch", "ignite"}, {"setonfire", "ignite"},
            {"freeze", "freeze"}, {"chill", "freeze"}, {"ice", "freeze"}, {"setfrozen", "freeze"},
            {"teleport", "teleport"}, {"tp", "teleport"}, {"warp", "teleport"}, {"relocate", "teleport"},
            {"give", "give"}, {"grant", "give"}, {"reward", "give"}, {"award", "give"},
            {"tell", "tell"}, {"msg", "tell"}, {"pm", "tell"}, {"whisper", "tell"},
            {"announce", "announce"}, {"broadcast", "announce"}, {"shout", "announce"},
            {"spawn", "spawn"}, {"summon", "spawn"}, {"materials", "spawn"}, {"create", "summon"},
            {"take", "take"}, {"confiscate", "take"}, {"strip", "take"},
            {"ban", "ban"}, {"eject", "kick"}, {"boot", "kick"}, {"removegame", "kick"},
            {"set", "set"}, {"assign", "set"}, {"make", "set"},
            {"weather", "weather"}, {"setweather", "weather"},
            {"time", "time"}, {"settime", "time"},
            {"hp", "health"}, {"sethealth", "sethealth"},
            {"fly", "setfly"}, {"setginvisible", "setinvisible"},
            {"op", "op"}, {"deop", "deop"},
            {"lightning", "strike"}, {"strikelightning", "strike"}, {"smite", "strike"},
            {"explode", "explode"}, {"blast", "explode"},
            {"feed", "feed"}, {"satiate", "feed"},
            {"win", "win"}, {"victory", "win"}, {"lose", "lose"}, {"defeat", "lose"},
            {"reset", "reset"}, {"wipe", "reset"},
            {"spawnmob", "spawn"}, {"spawner", "spawn"},
            {"kick", "kick"},
            {"giveeffect", "giveeffect"}, {"potion", "giveeffect"},
            {"enchant", "enchant"}, {"imbue", "enchant"},
            {"balance", "balance"}, {"coins", "balance"}, {"money", "balance"},
            {"define", "define"}, {"region", "define"}, {"area", "define"},
            {"buy", "buy"}, {"purchase", "buy"}, {"sell", "sell"},
            {"clear", "clear"}, {"delete", "clear"},
            {"title", "title"}, {"sendtitle", "title"},
            {"toast", "toast"}, {"notification", "toast"},
            {"actionbar", "actionbar"},
            {"welcome", "welcome"}, {"greet", "welcome"},
            {"killall", "killall"},
            {"killentities", "killall"},
            {"revive", "revive"}, {"resurrect", "revive"},
            {"setmute", "setmute"}, {"silence", "setmute"}, {"unmute", "unmute"},
            {"thunder", "strike"},
            {"say", "say"}, {"speak", "say"},
            {"drop", "drop"}, {"discard", "drop"}, {"toss", "drop"},
            {"throw", "throw"},
            {"healtofull", "healtofull"}, {"fullheal", "healtofull"},
            {"hidechat", "disablepublicchat"}, {"disablechat", "disablepublicchat"}, {"enablechat", "setpublicchat"},
            {"vanish", "vanish"}, {"unvanish", "unvanish"}, {"reveal", "reveal"},
            {"setinvincible", "setinvincible"}, {"godmode", "setinvincible"},
        };
        for (String[] row : s) SYNONYMS.put(row[0], row[1]);
    }

    // ---- scoreboards / teams / boss bars ---------------------------------------

    private static void scoreAction(Interpreter it, List<String> a, McRuntime r, boolean add, boolean remove, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 0) to = indexOfIgnoreCase(a, "by");
        List<String> before = to < 0 ? a : a.subList(0, to);
        double value = to >= 0 && to + 1 < a.size() ? parseNum(a.get(to + 1), line) : 1;
        int f = indexOfIgnoreCase(before, "for");
        int in = indexOfIgnoreCase(before, "in");
        String objective, target;
        if (f >= 0 && in > f) { objective = VariableStore.join(before.subList(in + 1, before.size())); target = target(it, before, f + 1); }
        else if (f >= 0) { objective = VariableStore.join(before.subList(0, f)); target = target(it, before, f + 1); }
        else { objective = VariableStore.join(before); target = it.focus(); }
        if (objective.isEmpty()) objective = "score";
        if (add) r.addScore(objective, target, value);
        else if (remove) r.removeScore(objective, target, value);
        else r.setScore(objective, target, value);
    }

    private static void teamMove(Interpreter it, List<String> a, McRuntime r, boolean add, int line) {
        int te = indexOfIgnoreCase(a, "team");
        String team = te >= 0 && te + 1 < a.size() ? VariableStore.join(a.subList(te + 1, a.size())) : "default";
        String who = it.focus();
        if (!a.isEmpty() && !a.get(0).equalsIgnoreCase("to")) who = target(it, a, 0);
        if (add) r.teamAdd(team, who); else r.teamRemove(team, who);
    }

    private static void teamValue(Interpreter it, List<String> a, McRuntime r, String kind, int line) {
        String team = a.isEmpty() ? "default" : a.get(0);
        String val = VariableStore.join(a.subList(1, a.size()));
        if (kind.equals("color")) r.teamColor(team, val);
        else if (kind.equals("prefix")) r.teamPrefix(team, val);
        else r.teamSuffix(team, val);
    }

    private static void bossBarValue(Interpreter it, List<String> a, McRuntime r, String kind, int line) {
        String name = a.isEmpty() ? "boss" : a.get(0);
        String val = VariableStore.join(a.subList(1, a.size()));
        if (kind.equals("color")) r.bossBarColor(name, val);
        else r.bossBarStyle(name, val);
    }

    private static void bossBarProgress(Interpreter it, List<String> a, McRuntime r, int line) {
        int bar = indexOfIgnoreCase(a, "bar");
        String name = bar >= 0 && bar + 1 < a.size() ? a.get(bar + 1) : "boss";
        double[] nums = numbersIn(a);
        double p = nums.length > 0 ? nums[nums.length - 1] : 0.5;
        r.bossBarProgress(name, p);
    }

    private static void worldRule(Interpreter it, List<String> a, McRuntime r, int line) {
        String rule = a.isEmpty() ? "disabled" : a.get(0);
        boolean on = !(containsWord(a, "off") || containsWord(a, "no") || containsWord(a, "false"));
        if (indexOfIgnoreCase(a, "on") >= 0 || containsWord(a, "true")) on = true;
        r.setWorldRule(rule, on);
    }

    // ---- item metadata (merged phrasal verbs) -----------------------------------

    private static void renameItemVerb(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 1) throw new VerbumError(line, "I need  rename item X to Y");
        String item = VariableStore.join(a.subList(0, to));
        String name = VariableStore.join(a.subList(to + 1, a.size()));
        it.runtime().renameItem(it.focus(), item, name);
    }

    private static void loreItemVerb(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 1) throw new VerbumError(line, "I need  set lore X to Y");
        String item = VariableStore.join(a.subList(0, to));
        String lore = VariableStore.join(a.subList(to + 1, a.size()));
        it.runtime().setLore(it.focus(), item, lore);
    }

    private static void modelDataVerb(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        if (to < 1 || to + 1 >= a.size()) throw new VerbumError(line, "I need  set model data X to 100");
        String item = VariableStore.join(a.subList(0, to));
        it.runtime().setCustomModelData(it.focus(), item, (int) parseNum(a.get(to + 1), line));
    }

    private static void setItemFlag(Interpreter it, List<String> a, McRuntime r, int line) {
        String item = a.isEmpty() ? "item" : a.get(0);
        String flag = a.size() > 1 ? a.get(1) : "unbreakable";
        boolean on = !(containsWord(a, "off") || containsWord(a, "false"));
        if (containsWord(a, "on") || containsWord(a, "true")) on = true;
        r.setItemFlag(it.focus(), item, flag, on);
    }

    // ---- misc helpers --------------------------------------------------------------

    private static double parseNum(String w, int line) {
        try { return Double.parseDouble(w.replace(",", "")); }
        catch (NumberFormatException e) { throw new VerbumError(line, "I expected a number, not  " + w); }
    }

    private static dev.verbum.runtime.Location locationAt(Interpreter it, List<String> a) {
        double[] nums = numbersIn(a);
        if (nums.length >= 3) return dev.verbum.runtime.Location.at("world", nums[0], nums[1], nums[2]);
        int at = indexOfIgnoreCase(a, "at");
        String t = at >= 0 && at + 1 < a.size() ? target(it, a, at + 1) : it.focus();
        return it.runtime().locationOf(t);
    }

    private static void makePlayer(Interpreter it, List<String> a, McRuntime r, int line) {
        int c = indexOfIgnoreCase(a, "command");
        String cmd = c >= 0 ? VariableStore.join(a.subList(c + 1, a.size())) : VariableStore.join(a);
        String t = it.focus();
        if (!a.isEmpty() && (a.get(0).equalsIgnoreCase("all") || a.get(0).equalsIgnoreCase("players")
                || a.get(0).equalsIgnoreCase("everyone"))) t = McRuntime.ALL;
        r.executeCommand(t, cmd);
    }

    private static void setMoney(Interpreter it, List<String> a, int line) {
        int to = indexOfIgnoreCase(a, "to");
        String player = target(it, a, 0);
        double amt = to >= 0 && to + 1 < a.size() ? parseNum(a.get(to + 1), line) : 0;
        var bag = it.store().playerVars(player.toLowerCase());
        bag.put("coins", amt);
    }

    private static void buy(Interpreter it, List<String> a, McRuntime r, int line) {
        int f = indexOfIgnoreCase(a, "for");
        String item = f >= 0 ? VariableStore.join(a.subList(0, f)).replaceFirst("(?i)^a ", "") : VariableStore.join(a);
        double price = f >= 0 && f + 1 < a.size() ? parseNum(a.get(f + 1), line) : firstNumber(a, line);
        var bag = it.store().playerVars(it.focus().toLowerCase());
        double cur = bag.containsKey("coins") ? it.store().asNumber(bag.get("coins"), line) : 0;
        if (cur < price) throw new VerbumError(line, "You do not have enough coins. You need " + (long) price);
        bag.put("coins", cur - price);
        r.give(it.focus(), item, 1);
    }

    private static void villagerPrice(Interpreter it, List<String> a, McRuntime r, int line) {
        int to = indexOfIgnoreCase(a, "to");
        String item = to >= 0 ? VariableStore.join(a.subList(0, to)) : VariableStore.join(a);
        double price = to >= 0 && to + 1 < a.size() ? parseNum(a.get(to + 1), line) : 1;
        r.setVillagerPrice("villager", item, price);
    }
}
