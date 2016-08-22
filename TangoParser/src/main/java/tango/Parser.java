package tango;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.common.proto.Demo.CDemoFileInfo;
import tango.util.PlayerInfo;
import tango.util.Position;
import tango.util.ReplayInfo;

import java.io.*;

public class Parser {
    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private final static int MAX_COORD_INTEGER = 16384; // use to covert to world coord
    private final static int CELL_OFFSET = 64;      // it make cell coord start from (0,0) at left bottom
    private final static int CELL_WIDTH = 1 << 7;   // the m_cellbits property had been remove in s2, it equals 7
    private final static int MAX_TICK = 2000;
    private final static int INTERVAL = 1;

    private ReplayInfo replayInfo = new ReplayInfo();
    private Gson gson = new Gson();
    private int time = 0;
    private int nextTime = 0;

    public Parser(InputStream input, OutputStream output) throws IOException {
        long tStart = System.currentTimeMillis();
        System.err.println("parser start");
        new SimpleRunner((new InputStreamSource(input))).runWith(this);
        long tEnd = System.currentTimeMillis();
        System.err.format("total time taken: %s\n", (tEnd - tStart)/1000.0);
        output.write(gson.toJson(replayInfo).getBytes());
    }

    @UsesEntities
    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
//        System.err.println("parse: tick start");
//        if (MAX_TICK > 0 && ctx.getTick() > MAX_TICK) {
//            return;
//        }

        Entity grp = ctx.getProcessor(Entities.class).getByDtName("CDOTAGamerulesProxy");
        Entity pr = ctx.getProcessor(Entities.class).getByDtName("CDOTA_PlayerResource");

        if (grp != null) {
            time = Math.round(getEntityProperty(grp, "m_pGameRules.m_fGameTime"));
            if (nextTime == 0) {
                nextTime = time;
            }
        }
        if (pr != null && time >= nextTime) {
            for (int i = 0; i < 10; i++) {
                int handle = getEntityProperty(pr, "m_vecPlayerTeamData.%i.m_hSelectedHero", i);
                Entity e = ctx.getProcessor(Entities.class).getByHandle(handle);
                if (e != null) {
                    Position position = new Position();
                    position.x = (int)getEntityProperty(e, "CBodyComponent.m_cellX") - CELL_OFFSET;
                    position.y = (int)getEntityProperty(e, "CBodyComponent.m_cellY") - CELL_OFFSET;
                    replayInfo.addPosition(i, position);
                }
            }
            nextTime = time + INTERVAL;
        }
    }

    @OnMessage(CDemoFileInfo.class)
    public void onFileInfo(Context ctx, CDemoFileInfo info) {
//        System.err.println("parse: file info");
        replayInfo.matchId = info.getGameInfo().getDota().getMatchId();
        replayInfo.dTime = (int)info.getPlaybackTime();
        replayInfo.gameMode = info.getGameInfo().getDota().getGameMode();
        replayInfo.winner = info.getGameInfo().getDota().getGameWinner();
        for (int i = 0; i < 10; i++) {
            PlayerInfo playerInfo = new PlayerInfo();
            Demo.CGameInfo.CDotaGameInfo.CPlayerInfo cPlayerInfo = info.getGameInfo().getDota().getPlayerInfo(i);
            playerInfo.heroName = transferName(cPlayerInfo.getHeroName());
            playerInfo.steamId = cPlayerInfo.getSteamid();
            replayInfo.addPlayer(i, playerInfo);
        }
    }

    private <T> T getEntityProperty(Entity e, String property, Integer idx) {
        if (e == null) {
            return null;
        }
        if (idx != null) {
            property = property.replace("%i", Util.arrayIdxToString(idx));
        }
        return e.getProperty(property);
    }

    private <T> T getEntityProperty(Entity e, String property) {
        return getEntityProperty(e, property, null);
    }

    // transfer heroName from npc_dota_hero_XXX to XXX
    private String transferName(String dtName) {
        return dtName.replaceFirst("npc_dota_hero_", "");
    }
}
