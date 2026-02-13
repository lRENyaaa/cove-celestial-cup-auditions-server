package top.rymc.phira.main.game;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.rymc.phira.main.Main;
import top.rymc.phira.main.data.ChartInfo;
import top.rymc.phira.main.data.GameRecord;
import top.rymc.phira.main.data.UserInfo;
import top.rymc.phira.main.network.request.PhiraFetcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class RaceScoreOperator {

    @Getter
    private final UserInfo userInfo;
    private final Map<Integer, List<GameRecord>> recordMap;

    public static RaceScoreOperator from(int playerId) {
        UserInfo userInfo = PhiraFetcher.GET_USER_INFO_BY_ID.toIntFunction((e) -> {
            throw new IllegalArgumentException("Íæ¼ÒID²»´æÔÚ");
        }).apply(playerId);

        List<Integer> maps = Main.getChartInfoList()
                .stream()
                .map(ChartInfo::getId)
                .toList();

        Map<Integer, List<GameRecord>> recordMap = maps.stream()
                .collect(Collectors.toMap(
                        map -> map,
                        map -> Main.getDao().findByPlayerAndChart(playerId, map)
                ));


        return new RaceScoreOperator(userInfo, recordMap);
    }

    public boolean hasRecord(int chartId) {
        return recordMap.get(chartId) != null && !recordMap.get(chartId).isEmpty();
    }

    public OptionalDouble getBestAccuracy(int chartId) {
        return recordMap.get(chartId)
                .stream()
                .mapToDouble(GameRecord::getAccuracy)
                .max();
    }

    public OptionalDouble getBestStd(int chartId) {
        return recordMap.get(chartId)
                .stream()
                .mapToDouble(GameRecord::getStd)
                .min();
    }

    public OptionalInt getBestScore(int chartId) {
        return recordMap.get(chartId)
                .stream()
                .mapToInt(GameRecord::getScore)
                .max();
    }


    public record ScoreInfo(int score, double accuracy, double std) {
    }

    public Optional<ScoreInfo> getBestScoreInfo(int chartId) {
        OptionalDouble bestAccuracy = getBestAccuracy(chartId);
        OptionalDouble bestStd = getBestStd(chartId);
        OptionalInt bestScore = getBestScore(chartId);

        if (bestAccuracy.isPresent() && bestStd.isPresent() && bestScore.isPresent()) {
            return Optional.of(new ScoreInfo(bestScore.getAsInt(), bestAccuracy.getAsDouble(), bestStd.getAsDouble()));
        } else {
            return Optional.empty();
        }
    }




}
