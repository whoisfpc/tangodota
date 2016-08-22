package tango.util;

import java.util.ArrayList;

public class ReplayInfo {
    public long matchId;
    public int dTime;
    public int gameMode;
    public int winner;
    private ArrayList<PlayerInfo> players = new ArrayList<PlayerInfo>(10);
    private ArrayList<ArrayList<Position>> positions = new ArrayList<>(10);

    public ReplayInfo() {
        for (int i = 0; i < 10; i++) {
            positions.add(new ArrayList<>());
        }
    }

    public void addPosition(int i, Position position) {
        positions.get(i).add(position);
    }

    public void addPlayer(int i, PlayerInfo playerInfo) {
        players.add(i, playerInfo);
    }
}
