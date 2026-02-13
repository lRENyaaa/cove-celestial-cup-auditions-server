package top.rymc.phira.main.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public final class ChartInfo {
    private int id;
    private String name;
    private String level;
    private float difficulty;
    private String charter;
    private String composer;
    private String illustrator;
    private String description;
    private boolean ranked;
    private boolean reviewed;
    private boolean stable;
    private boolean stableRequest;
    private String illustration;
    private String preview;
    private String file;
    private int uploader;
    private String[] tags;
    private float rating;
    private int ratingCount;
    private OffsetDateTime created;
    private OffsetDateTime updated;
    private OffsetDateTime chartUpdated;
}

