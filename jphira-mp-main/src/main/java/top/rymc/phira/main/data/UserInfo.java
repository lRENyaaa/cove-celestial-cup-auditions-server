package top.rymc.phira.main.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.OffsetDateTime;

@Getter
@ToString
@EqualsAndHashCode
@SuppressWarnings("unused")
public class UserInfo {

    private int id;
    private String name;
    private String avatar;
    private String language;
    private String bio;
    private int exp;
    private double rks;
    private OffsetDateTime joined;
    private OffsetDateTime lastLogin;
    private int roles;
    private boolean banned;
    private boolean loginBanned;
    private int followerCount;
    private int followingCount;
    private String email;

}