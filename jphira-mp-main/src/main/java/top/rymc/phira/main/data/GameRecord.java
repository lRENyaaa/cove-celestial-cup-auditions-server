package top.rymc.phira.main.data;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class GameRecord {
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

