package dev.verbum.parser;

import dev.verbum.ast.*;
import dev.verbum.error.VerbumError;
import dev.verbum.i18n.KeywordResolver;
import dev.verbum.lex.Line;
import dev.verbum.lex.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * A hand-written, recursive-descent parser for Verbum.
 *
 * The parser walks the tokenized lines. A header word ("when", "every", "on",
 * "action", "if", "repeat", "for", "until", ...) starts something; a deeper
 * indentation starts its block. Every other line is an action call whose first
 * word is the verb and the rest is kept as free text.
 */
public final class Parser {

    private final List<Line> lines;
    private int cursor = 0;

    public Parser(List<Line> lines) {
        this(lines, KeywordResolver.ENGLISH);
    }

    /**
     * Parses using a keyword resolver so the first word of every line can be
     * written in another language ("dar" -> "give", "töte" -> "kill", ...).
     * Only the first word of each line is rewritten; arguments and free text
     * are left exactly as the author wrote them.
     */
    public Parser(List<Line> lines, KeywordResolver resolver) {
        this.lines = normalizeFirstWords(lines, resolver);
    }

    private static List<Line> normalizeFirstWords(List<Line> lines, KeywordResolver resolver) {
        List<Line> out = new ArrayList<>(lines.size());
        for (Line line : lines) {
            List<Token> tokens = line.tokens();
            if (tokens.isEmpty()) { out.add(line); continue; }
            Token first = tokens.get(0);
            String resolved = resolver.resolve(first.text());
            if (resolved.equals(first.text())) {
                out.add(line);
            } else {
                List<Token> normalized = new ArrayList<>(tokens);
                normalized.set(0, Token.word(resolved));
                out.add(new Line(line.lineNumber(), line.indent(), normalized));
            }
        }
        return out;
    }


    public Program parse() {
        Program program = new Program();
        while (cursor < lines.size()) {
            Line line = lines.get(cursor);
            if (line.indent() != 0) {
                throw new VerbumError(line.lineNumber(),
                        "I found an indented line with nothing before it.\n" +
                        "An indented line must sit under a line that starts with  when, every, on, action, function, command, if, repeat, for, until  or  return.");
            }
            parseTopLevel(program);
        }
        return program;
    }

    private void parseTopLevel(Program program) {
        Line line = lines.get(cursor);
        String first = firstWord(line);

        if (first.equalsIgnoreCase("action") || first.equalsIgnoreCase("function")) {
            program.addAction(parseCustomAction());
            return;
        }
        if (first.equalsIgnoreCase("command")) {
            program.addCommand(parseCommand());
            return;
        }
        if (first.equalsIgnoreCase("menu")) {
            program.addMenu(parseMenu());
            return;
        }
        EventHandler h = parseEvent();
        if (h == null) {
            throw new VerbumError(line.lineNumber(),
                    "I expected a line that starts with  when, every, on, action, command  or  menu.\n" +
                    "Example:\n  when player joins\n      tell player welcome");
        }
        program.addEvent(h);
    }

    // ---- user-defined commands --------------------------------------------

    private CommandHandler parseCommand() {
        Line line = lines.get(cursor);
        List<String> words = words(line);
        cursor++;
        if (words.size() < 2) {
            throw new VerbumError(line.lineNumber(),
                    "I need a command name after  command, like\n  command hello\n      tell player Hello");
        }
        String name = words.get(1).toLowerCase();
        List<String> params = new ArrayList<>();
        for (int i = 2; i < words.size(); i++) params.add(words.get(i).toLowerCase());
        Block body = parseBlock(line.indent());
        return new CommandHandler(line.lineNumber(), name, params, body);
    }

    // ---- custom menus (GUIs) ----------------------------------------------

    private MenuBlock parseMenu() {
        Line line = lines.get(cursor);
        List<String> words = words(line);
        cursor++;
        if (words.size() < 2) {
            throw new VerbumError(line.lineNumber(),
                    "I need a menu name after  menu, like\n  menu rewards\n      add button Unlock Sword\n          give player diamond sword");
        }
        String name = words.get(1).toLowerCase();
        MenuBlock menu = new MenuBlock(line.lineNumber(), name);
        while (cursor < lines.size() && lines.get(cursor).indent() > line.indent()) {
            Line b = lines.get(cursor);
            List<String> bwords = words(b);
            cursor++;
            // "add button Label ..."  (drop leading "add")
            int idx = 0;
            if (idx < bwords.size() && bwords.get(idx).equalsIgnoreCase("add")) idx++;
            if (idx >= bwords.size() || !bwords.get(idx).equalsIgnoreCase("button")) {
                throw new VerbumError(b.lineNumber(),
                        "Inside a menu I expect  add button <label>\nExample:  add button Unlock Sword");
            }
            String label = joinFrom(bwords, idx + 1);
            Block body = parseBlock(b.indent());
            menu.addButton(new MenuBlock.Button(label, b.lineNumber(), body));
        }
        return menu;
    }

    // ---- events ---------------------------------------------------------

    private EventHandler parseEvent() {
        Line line = lines.get(cursor);
        String first = firstWord(line);
        List<String> words = words(line);

        if (first.equalsIgnoreCase("on")) {
            cursor++;
            String trigger = joinWords(words, 1);
            Block body = parseBlock(line.indent());
            EventHandler.Kind kind = EventHandler.Kind.ON;
            if (trigger.equalsIgnoreCase("server start")) return new EventHandler(line.lineNumber(), kind, null, List.of("server","start"), null, body);
            if (trigger.equalsIgnoreCase("server stop"))  return new EventHandler(line.lineNumber(), kind, null, List.of("server","stop"), null, body);
            throw new VerbumError(line.lineNumber(),
                    "I do not know the event  on " + trigger + "\n" +
                    "I know  on server start  and  on server stop.");
        }

        if (first.equalsIgnoreCase("every")) {
            // every 5 seconds / every minute / every 10 minutes
            cursor++;
            if (words.size() < 2) {
                throw new VerbumError(line.lineNumber(),
                        "After  every  I need an amount, like  every 5 seconds.");
            }
            Token t = lines.get(cursor - 1).tokens().get(1);
            if (t.type() != Token.Type.NUMBER) {
                throw new VerbumError(line.lineNumber(),
                        "I expected a number after  every.\nExample:  every 5 seconds");
            }
            long base = t.number().longValue();
            String unit = words.size() > 2 ? words.get(2) : "seconds";
            long seconds = toSeconds(unit, base, line.lineNumber());
            Block body = parseBlock(line.indent());
            return new EventHandler(line.lineNumber(), EventHandler.Kind.EVERY, (int) seconds,
                    null, null, body);
        }

        if (first.equalsIgnoreCase("when")) {
            if (words.size() < 2) {
                throw new VerbumError(line.lineNumber(),
                        "After  when  I need a situation, like  when player joins.");
            }
            cursor++;
            List<String> condition = new ArrayList<>(words.subList(1, words.size()));
            int priority = 0;
            // optional priority qualifier:  when player joins priority high
            int pIdx = indexOfInsensitive(condition, 0, "priority");
            if (pIdx >= 0) {
                String lvl = condition.size() > pIdx + 1 ? condition.get(pIdx + 1) : "";
                if (lvl.equalsIgnoreCase("high") || lvl.equalsIgnoreCase("highest")
                        || lvl.equalsIgnoreCase("first")) priority = 1;
                else if (lvl.equalsIgnoreCase("low") || lvl.equalsIgnoreCase("lowest")
                        || lvl.equalsIgnoreCase("last")) priority = -1;
                else if (!lvl.equalsIgnoreCase("normal") && !lvl.equalsIgnoreCase("middle")) {
                    throw new VerbumError(line.lineNumber(),
                            "I expected  high,  normal  or  low  after  priority.\nExample:  when player joins priority high");
                }
                List<String> trimmed = new ArrayList<>(condition.subList(0, pIdx));
                trimmed.addAll(condition.subList(pIdx + 2, condition.size()));
                condition = trimmed;
            }
            Block body = parseBlock(line.indent());
            return new EventHandler(line.lineNumber(), EventHandler.Kind.WHEN, null, null,
                    condition, priority, body);
        }

        return null;
    }

    private long toSeconds(String unit, long base, int line) {
        unit = unit.toLowerCase();
        if (unit.startsWith("second")) return base;
        if (unit.startsWith("minute")) return base * 60;
        if (unit.startsWith("hour")) return base * 3600;
        throw new VerbumError(line, "I do not understand  " + unit + "\nTry  seconds,  minutes  or  hours.");
    }

    // ---- custom actions -------------------------------------------------

    private CustomAction parseCustomAction() {
        Line line = lines.get(cursor);
        List<String> words = words(line);
        cursor++;
        if (words.size() < 2) {
            throw new VerbumError(line.lineNumber(),
                    "After  action  I need a name, like  action reward player.");
        }
        String name = words.get(1).toLowerCase();
        List<String> params = new ArrayList<>();
        // skip the word  and  so  function double a and b  binds parameters a and b
        for (int i = 2; i < words.size(); i++) {
            String p = words.get(i).toLowerCase();
            if (p.equals("and") || p.equals("with")) continue;
            params.add(p);
        }
        Block body = parseBlock(line.indent());
        return new CustomAction(line.lineNumber(), name, params, body);
    }

    // ---- statements inside blocks ----------------------------------------

    private Stmt parseStatement(int blockIndent) {
        Line line = lines.get(cursor);
        if (line.indent() <= blockIndent) return null; // not part of this block
        String first = firstWord(line);
        List<String> words = words(line);

        switch (first.toLowerCase()) {
            case "if":
            case "unless": {
                cursor++;
                boolean negate = first.equalsIgnoreCase("unless");
                List<String> cond = new ArrayList<>(words.subList(1, words.size()));
                if (negate) cond.add(0, "not");
                List<List<String>> conds = new ArrayList<>();
                List<Block> bodies = new ArrayList<>();
                conds.add(cond);
                Block body = parseBlock(line.indent());
                bodies.add(body);
                Block elseBody = new Block();
                // look ahead for else / else if at the same indent as the if
                while (peekIndent() == line.indent() && cursor < lines.size()
                        && firstWord(lines.get(cursor)).equalsIgnoreCase("else")) {
                    Line e = lines.get(cursor);
                    List<String> ewords = words(e);
                    cursor++;
                    if (ewords.size() > 1 && ewords.get(1).equalsIgnoreCase("if")) {
                        List<String> c2 = new ArrayList<>(ewords.subList(2, ewords.size()));
                        conds.add(c2);
                        bodies.add(parseBlock(e.indent()));
                    } else {
                        elseBody = parseBlock(e.indent());
                        break;
                    }
                }
                return new IfStmt(line.lineNumber(), conds, bodies, elseBody);
            }
            case "repeat": {
                cursor++;
                if (words.size() < 2) {
                    throw new VerbumError(line.lineNumber(), "After  repeat  I need a count or  while.\nExample:  repeat 10 times  or  repeat while player has less than 5 diamonds.");
                }
                String second = words.get(1);
                if (second.equalsIgnoreCase("while")) {
                    List<String> cond = new ArrayList<>(words.subList(2, words.size()));
                    Block b = parseBlock(line.indent());
                    return new LoopCondition(line.lineNumber(), LoopCondition.Mode.WHILE, cond, b);
                }
                // repeat N times
                if (second.equalsIgnoreCase("forever") || second.equalsIgnoreCase("always")) {
                    Block b = parseBlock(line.indent());
                    return new RepeatTimes(line.lineNumber(), List.of("999999"), b);
                }
                // repeat <number> [times]
                Token t = line.tokens().get(1);
                if (t.type() != Token.Type.NUMBER) {
                    throw new VerbumError(line.lineNumber(), "I expected a number after  repeat.\nExample:  repeat 10 times");
                }
                List<String> count = new ArrayList<>(words.subList(1, 2));
                Block b = parseBlock(line.indent());
                return new RepeatTimes(line.lineNumber(), count, b);
            }
            case "until": {
                cursor++;
                if (words.size() < 2) {
                    throw new VerbumError(line.lineNumber(), "After  until  I need a situation.\nExample:  until player reaches Castle");
                }
                List<String> cond = new ArrayList<>(words.subList(1, words.size()));
                Block b = parseBlock(line.indent());
                return new LoopCondition(line.lineNumber(), LoopCondition.Mode.UNTIL, cond, b);
            }
            case "for": case "loop": {
                cursor++;
                // words includes the leading  for / loop  keyword
                String item = "player";
                List<String> rest = new ArrayList<>(words.subList(1, words.size()));
                List<String> list;
                if (!rest.isEmpty() && rest.get(0).equalsIgnoreCase("each")) {
                    // for each ITEM in LIST   /   for each online player
                    List<String> inner = new ArrayList<>(rest.subList(1, rest.size()));
                    int inIndex = indexOfInsensitive(inner, 0, "in");
                    if (inIndex >= 0) {
                        item = inner.get(0).toLowerCase();
                        list = new ArrayList<>(inner.subList(inIndex + 1, inner.size()));
                    } else {
                        // "for each online player" -> iterate players, item is "player"
                        item = "player";
                        list = inner;
                    }
                } else {
                    // loop all players   /   loop player in online players
                    int inIndex = indexOfInsensitive(rest, 0, "in");
                    if (inIndex >= 0) {
                        item = rest.get(0).toLowerCase();
                        list = new ArrayList<>(rest.subList(inIndex + 1, rest.size()));
                    } else {
                        item = "player";
                        list = rest;
                    }
                }
                if (list.isEmpty()) {
                    throw new VerbumError(line.lineNumber(),
                            "I need something to loop over, like  loop all players  or  for each item in player's list.");
                }
                Block b = parseBlock(line.indent());
                return new ForEach(line.lineNumber(), item, list, b);
            }
            case "break":
                cursor++;
                return new Flow(line.lineNumber(), Flow.Kind.BREAK);
            case "continue":
            case "skip":
                cursor++;
                return new Flow(line.lineNumber(), Flow.Kind.CONTINUE);
            case "stop":
                cursor++;
                return new Flow(line.lineNumber(), Flow.Kind.STOP);
            case "return": {
                cursor++;
                //  return  with nothing returns an empty value;  return X  returns the value
                List<String> valueWords = words.size() > 1 ? new ArrayList<>(words.subList(1, words.size())) : new ArrayList<>();
                return new Return(line.lineNumber(), valueWords);
            }
            case "after": case "later": {
                // after 5 seconds <block>   |   later 3 minutes <block>
                cursor++;
                if (words.size() < 3) {
                    throw new VerbumError(line.lineNumber(),
                            "After  " + first + "  I need a time, like  after 5 seconds.");
                }
                List<String> delay = new ArrayList<>(words.subList(1, words.size()));
                Block b = parseBlock(line.indent());
                return new DelayedBlock(line.lineNumber(), delay, b);
            }
            default:
                // action call: verb is first word, rest are args
                cursor++;
                String verb;
                List<String> args;
                String[] norm = normalizeVerb(words);
                verb = norm[0];
                int consumed = Integer.parseInt(norm[1]);
                List<String> kept = new ArrayList<>();
                for (int k = consumed; k < words.size(); k++) kept.add(words.get(k));
                args = kept;
                return new ActionCall(line.lineNumber(), verb, args);
        }
    }

    // ------------------------------------------------------------- verbs

    /**
     * Merges multi-word verb phrases ("set gamemode", "strike lightning") into
     * a canonical single verb so the action library can grow by phrases, not
     * just single words. Returns {canonicalVerb, wordsConsumed}.
     */
    static String[] normalizeVerb(List<String> words) {
        if (words.isEmpty()) return new String[]{"", "0"};
        String first = words.get(0).toLowerCase();
        for (int len = Math.min(4, words.size()); len >= 2; len--) {
            StringBuilder key = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0) key.append(' ');
                key.append(words.get(i).toLowerCase());
            }
            String canonical = MULTI_WORD_VERBS.get(key.toString());
            if (canonical != null) {
                return new String[]{canonical, String.valueOf(len)};
            }
        }
        return new String[]{first, "1"};
    }

    /** Canonical verb -> merged phrase. Both directions are real, accepted English. */
    private static final java.util.Map<String, String> MULTI_WORD_VERBS = new java.util.LinkedHashMap<>();
    static {
        String[][] phrases = {
            // world & weather
            {"set weather", "setweather"}, {"make it rain", "setweather"}, {"make rain", "setweather"},
            {"set time", "settime"}, {"make it night", "settime"}, {"make night", "settime"},
            {"set block", "setblock"}, {"place block", "setblock"}, {"place a block", "setblock"},
            {"break block", "breakblock"}, {"destroy block", "breakblock"}, {"mine block", "breakblock"},
            {"play sound", "playsound"}, {"play a sound", "playsound"},
            {"play particle", "playparticle"}, {"play particles", "playparticle"},
            {"strike lightning", "strikelightning"}, {"strike with lightning", "strikelightning"},
            {"set border", "setborder"}, {"set world border", "setborder"},
            {"set difficulty", "setdifficulty"}, {"set difficulty to", "setdifficulty"},
            {"set spawn point", "setspawnpoint"}, {"set spawn", "setspawnpoint"},
            {"set world spawn", "setspawnpoint"},
            {"set mob limit", "setmoblimit"}, {"set mob cap", "setmoblimit"},
            {"set explosion damage", "setexplosiondamage"},
            // movement & state
            {"set gamemode", "setgamemode"}, {"set game mode", "setgamemode"},
            {"set fly", "setfly"}, {"make fly", "setfly"}, {"set flying", "setfly"},
            {"set walk speed", "setwalkspeed"}, {"set speed", "setwalkspeed"},
            {"set fly speed", "setflyspeed"},
            {"set mob health", "setmobhealth"}, {"set boss health", "setmobhealth"},
            {"set mob speed", "setmobspeed"}, {"set boss speed", "setmobspeed"},
            {"set mob hostility", "setmobhostility"}, {"make mob hostile", "setmobhostility"},
            {"make hostile", "setmobhostility"},
            {"set on fire", "setonfire"}, {"put out fire", "extinguish"}, {"extinguish fire", "extinguish"},
            {"set frozen", "setfrozen"}, {"make freeze", "setfrozen"},
            {"teleport to", "teleport"}, {"tp to", "teleport"},
            // items & inventory
            {"give permission", "givepermission"}, {"grant permission", "givepermission"},
            {"remove permission", "removepermission"}, {"revoke permission", "removepermission"},
            {"give effect", "giveeffect"}, {"apply effect", "giveeffect"},
            {"remove effect", "removeeffect"}, {"clear effect", "removeeffect"},
            {"give xp", "givexp"}, {"give experience", "givexp"}, {"grant xp", "givexp"},
            {"give levels", "givelevels"}, {"add levels", "givelevels"},
            {"give item", "give"}, {"give an item", "give"}, {"give items", "give"},
            {"take item", "take"}, {"take away", "take"},
            {"clear inventory", "clearinventory"}, {"clear inv", "clearinventory"},
            {"set item", "setitem"}, {"set item name", "renameitem"},
            {"rename item", "renameitem"}, {"rename to", "renameitem"},
            {"set lore", "setlore"}, {"add lore", "setlore"}, {"set item lore", "setlore"},
            {"set model data", "setmodeldata"}, {"set custom model data", "setmodeldata"},
            {"set model", "setmodeldata"},
            {"set amount", "setamount"},
            {"set item flags", "setitemflags"}, {"add item flag", "setitemflags"},
            {"give helmet", "givehelmet"}, {"give armor", "givearmor"},
            {"equip armor", "givearmor"}, {"set armor", "givearmor"},
            {"set held item", "sethelditem"}, {"set hand item", "sethelditem"},
            {"set offhand item", "setoffhanditem"},
            {"drop item", "drop"}, {"throw item", "drop"},
            {"throw a pearl", "throw"}, {"throw pearl", "throw"},
            {"shoot arrow", "shoot"}, {"shoot an arrow", "shoot"},
            {"shoot firework", "shootfirework"}, {"launch firework", "shootfirework"},
            // scoreboards / teams
            {"set score", "setscore"}, {"add score", "addscore"}, {"remove score", "removescore"},
            {"create scoreboard", "createscoreboard"}, {"make scoreboard", "createscoreboard"},
            {"create objective", "createscoreboard"},
            {"create team", "createteam"}, {"make team", "createteam"},
            {"add to team", "addtoteam"}, {"join team", "addtoteam"},
            {"remove from team", "removefromteam"}, {"kick from team", "removefromteam"},
            {"set team color", "setteamcolor"}, {"set team prefix", "setteamprefix"},
            {"set team suffix", "setteamsuffix"},
            {"set scoreboard display", "setscoreboarddisplay"},
            {"set tab list", "settablist"}, {"set tab list name", "settablist"},
            {"set player list name", "settablist"},
            // boss bar
            {"create boss bar", "createbossbar"}, {"make boss bar", "createbossbar"},
            {"set boss bar", "setbossbar"}, {"set boss bar title", "setbossbar"},
            {"set boss bar color", "setbossbarcolor"}, {"set boss bar style", "setbossbarstyle"},
            {"set boss bar progress", "setbossbarprogress"}, {"show boss bar", "showbossbar"},
            {"hide boss bar", "hidebossbar"}, {"remove boss bar", "removebossbar"},
            // chat & ui
            {"send title", "title"}, {"show title", "title"},
            {"send subtitle", "subtitle"}, {"show subtitle", "subtitle"},
            {"send actionbar", "actionbar"}, {"show actionbar", "actionbar"},
            {"send toast", "toast"}, {"show toast", "toast"},
            {"send message", "tell"}, {"send a message", "tell"}, {"pm to", "tell"},
            {"send warning", "warn"}, {"warn player", "warn"},
            {"send announce", "announce"}, {"announce to all", "announce"},
            {"broadcast message", "announce"},
            {"open menu", "openmenu"}, {"open gui", "openmenu"},
            {"open anvil", "openanvil"}, {"open workbench", "openworkbench"},
            {"open crafting table", "openworkbench"}, {"open shop", "openshop"},
            {"open inventory", "openinventory"}, {"show inventory", "openinventory"},
            // life & body
            {"heal to full", "healtofull"}, {"heal fully", "healtofull"}, {"full heal", "healtofull"},
            {"set health", "sethealth"}, {"set max health", "setmaxhealth"},
            {"set food", "setfood"}, {"set hunger", "setfood"},
            {"set level", "setlevel"}, {"set xp level", "setlevel"},
            {"set speed", "setwalkspeed"}, {"set walk speed", "setwalkspeed"},
            {"set fly speed", "setflyspeed"},
            {"make invisible", "setinvisible"}, {"set invisible", "setinvisible"},
            {"make visible", "setvisible"}, {"set visible", "setvisible"},
            {"make glowing", "setglowing"}, {"set glowing", "setglowing"},
            {"make invulnerable", "setinvulnerable"}, {"set invulnerable", "setinvulnerable"},
            {"set gravity", "setgravity"}, {"enable gravity", "setgravity"}, {"disable gravity", "setgravity"},
            {"set op", "setop"}, {"make op", "setop"}, {"give op", "setop"},
            {"set operator", "setop"},
            {"set mute", "setmute"}, {"mute player", "setmute"}, {"silence player", "setmute"},
            {"unmute player", "unmute"}, {"unmute", "unmute"},
            {"unban player", "unban"}, {"pardon player", "unban"}, {"pardon", "unban"},
            {"revive player", "revive"}, {"bring back to life", "revive"},
            // economy & shop
            {"pay to", "pay"}, {"give money", "pay"}, {"pay money", "pay"},
            {"charge player", "charge"}, {"take money", "charge"},
            {"deposit money", "deposit"}, {"withdraw money", "withdraw"},
            {"check balance", "balance"}, {"get balance", "balance"},
            {"set money", "setmoney"}, {"set balance", "setmoney"},
            {"buy item", "buy"}, {"sell item", "sell"}, {"set shop price", "setshopprice"},
            // creatures
            {"spawn mob", "spawn"}, {"spawn a mob", "spawn"}, {"summon mob", "spawn"}, {"summon", "spawn"},
            {"despawn mob", "despawn"}, {"remove mob", "despawn"},
            {"make mob", "makemob"}, {"tame mob", "tamemob"}, {"tame animal", "tamemob"},
            {"name mob", "namemob"}, {"name a mob", "namemob"}, {"set mob name", "namemob"},
            {"set mob age", "setmobage"}, {"set mob size", "setmobsize"},
            {"set mob pitch", "setmobpitch"},
            // doors / redstone
            {"open door", "opendoor"}, {"open a door", "opendoor"},
            {"close door", "closedoor"}, {"close a door", "closedoor"},
            {"open gate", "opengate"}, {"close gate", "closegate"},
            {"open trapdoor", "opentrapdoor"}, {"close trapdoor", "closetrapdoor"},
            {"set redstone", "setredstone"}, {"power redstone", "setredstone"},
            {"power block", "setredstone"},
            // game
            {"win game", "wingame"}, {"win the game", "wingame"},
            {"lose game", "losegame"}, {"lose the game", "losegame"},
            {"start game", "startgame"}, {"start minigame", "startgame"},
            {"end game", "endgame"}, {"stop game", "endgame"}, {"finish game", "endgame"},
            {"set round", "setround"}, {"next round", "nextround"},
            // misc commands
            {"make player", "makeplayer"}, {"make all", "makeplayer"},
            {"execute command", "executecommand"}, {"run command", "executecommand"},
            {"ban player", "ban"}, {"kick player", "kick"}, {"kick for", "kick"},
            {"send to lobby", "sendtolobby"}, {"send to hub", "sendtolobby"},
            {"send to server", "sendtoserver"}, {"connect to server", "sendtoserver"},
            {"set click command", "setclickcommand"},
            {"set sign text", "setsigntext"}, {"edit sign", "setsigntext"},
            {"set world time", "settime"}, {"set world weather", "setweather"},
            {"set day", "settime"}, {"set night", "settime"}, {"set noon", "settime"}, {"set midnight", "settime"},
            {"create zone", "define"}, {"define area", "define"}, {"create area", "define"}, {"add region", "define"},
            {"set villager price", "setvillagerprice"}, {"set villager trade", "setvillagerprice"},
            {"set quest", "setquest"}, {"complete quest", "completequest"}, {"finish quest", "completequest"},
            {"set flag", "setflag"}, {"toggle flag", "toggleflag"},
            {"set world rule", "setworldrule"}, {"set gamerule", "setworldrule"},
            // chat & messages
            {"set join message", "setjoinmessage"}, {"set quit message", "setquitmessage"},
            {"set public chat", "setpublicchat"}, {"enable public chat", "setpublicchat"},
            {"disable public chat", "disablepublicchat"}, {"disable chat", "disablepublicchat"}, {"hide chat", "disablepublicchat"},
            {"set private chat", "setprivatechat"}, {"disable private chat", "disableprivatechat"}, {"disable pm", "disableprivatechat"},
            {"clear chat", "clearchat"}, {"clear the chat", "clearchat"},
            {"send hover message", "sendhovermessage"}, {"send hover text", "sendhovermessage"},
            {"send click message", "sendclickmessage"}, {"send clickable text", "sendclickmessage"},
            {"set clickable text", "sendclickmessage"},
            // player meta & state
            {"set sneaking", "setsneaking"}, {"make sneak", "makesneak"}, {"make player sneak", "makesneak"},
            {"set sprinting", "setsprinting"}, {"make sprint", "makesprint"}, {"make player sprint", "makesprint"},
            {"vanish player", "vanish"}, {"make vanish", "vanish"},
            {"hide player", "hide"}, {"show player", "show"},
            {"set armor points", "setarmorpoints"},
            {"set absorption", "setabsorption"}, {"set absorption hearts", "setabsorption"},
            {"cancel fall damage", "cancelfalldamage"}, {"set fall protection", "setfallprotection"},
            {"set invincible", "setinvincible"}, {"make invincible", "setinvincible"}, {"set god mode", "setinvincible"},
            {"set cooldown", "setcooldown"}, {"put cooldown", "setcooldown"},
            // world & environment
            {"set weather duration", "setweatherduration"}, {"set storm", "setstorm"}, {"make storm", "setstorm"},
            {"set thunder", "setthunder"}, {"make thunder", "setthunder"},
            {"set time speed", "settimespeed"}, {"set day cycle", "settimespeed"},
            {"set player limit", "setplayerlimit"}, {"set slots", "setslots"}, {"set server slots", "setslots"},
            {"spawn structure", "spawnstructure"}, {"place structure", "spawnstructure"},
            // entities & mobs
            {"set mob ai", "setmobai"}, {"disable mob ai", "setmobai"},
            {"set mob gravity", "setmobgravity"}, {"disable mob gravity", "setmobgravity"},
            {"set mob flying", "setmobflying"}, {"make mob fly", "setmobflying"},
            {"set mob breeding", "setmobbreeding"}, {"disable breeding", "setmobbreeding"},
            {"set mob drop", "setmobdrop"}, {"set custom drop", "setmobdrop"}, {"make mob drop", "setmobdrop"},
            {"set mob follow", "setmobfollow"}, {"make mob follow", "setmobfollow"},
            {"set mob target", "setmobtarget"}, {"set mob name visible", "setmobnamevisible"},
            {"set mob persistent", "setmobpersistent"}, {"make mob persistent", "setmobpersistent"},
            // systems & logic
            {"set sidebar title", "setsidebartitle"}, {"set scoreboard title", "setsidebartitle"},
            {"set sidebar line", "setsidebarline"}, {"set scoreboard line", "setsidebarline"},
            // stats & fireworks
            {"shoot colored firework", "shootcoloredfirework"}, {"launch colored firework", "shootcoloredfirework"},
            {"add a kill", "addkill"}, {"add kill", "addkill"}, {"record a kill", "addkill"},
            {"add a death", "adddeath"}, {"add death", "adddeath"}, {"record a death", "adddeath"},
        };
        for (String[] p : phrases) MULTI_WORD_VERBS.put(p[0], p[1]);
    }

    // ---- block building ---------------------------------------------------

    /** Reads all consecutive lines indented deeper than the header line. */
    private Block parseBlock(int headerIndent) {
        Block block = new Block();
        while (cursor < lines.size()) {
            Line line = lines.get(cursor);
            if (line.indent() <= headerIndent) break;
            Stmt s = parseStatement(headerIndent);
            if (s == null) break;
            block.add(s);
        }
        return block;
    }

    // ---- helpers -----------------------------------------------------------

    private int peekIndent() {
        if (cursor >= lines.size()) return Integer.MAX_VALUE;
        return lines.get(cursor).indent();
    }

    private static String firstWord(Line line) {
        return line.tokens().get(0).text();
    }

    private static List<String> words(Line line) {
        List<String> out = new ArrayList<>();
        for (Token t : line.tokens()) out.add(t.text());
        return out;
    }

    private static String joinWords(List<String> words, int from) {
        return joinFrom(words, from);
    }

    private static String joinFrom(List<String> words, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < words.size(); i++) {
            if (i > from) sb.append(' ');
            sb.append(words.get(i));
        }
        return sb.toString();
    }

    private static int indexOfInsensitive(List<String> words, int from, String target) {
        for (int i = from; i < words.size(); i++) {
            if (words.get(i).equalsIgnoreCase(target)) return i;
        }
        return -1;
    }
}
