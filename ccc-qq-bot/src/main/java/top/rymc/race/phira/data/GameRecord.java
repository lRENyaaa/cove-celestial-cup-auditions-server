package top.rymc.race.phira.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public final class GameRecord {
    private int id;
    private int player;
    private int chart;
    private int score;
    private float accuracy;
    private int perfect;
    private int good;
    private int bad;
    private int miss;
    private float speed;
    private int maxCombo;
    private boolean best;
    private boolean bestStd;
    private int mods;
    private boolean fullCombo;
    private OffsetDateTime time;
    private float std;
    private float stdScore;
}

