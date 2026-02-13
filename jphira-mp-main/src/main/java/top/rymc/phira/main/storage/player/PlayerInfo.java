package top.rymc.phira.main.storage.player;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInfo {

    @Expose
    private int id;
    @Expose
    private String qq;
    @Expose
    private boolean autoContinue;
    @Expose
    private double joinWait;

    public String getFormattedJoinWait() {
        return formatDouble(joinWait, 4);
    }

    public static String formatDouble(double value, int maxFractionDigits) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }

        BigDecimal bd = BigDecimal.valueOf(value);

        bd = bd.setScale(maxFractionDigits, RoundingMode.HALF_UP);
        bd = bd.stripTrailingZeros();

        return bd.toPlainString();
    }

    public int getJoinWaitInMilliseconds() {
        int wait = (int) (joinWait * 1000);

        if (wait < 1000) {
            wait = 1000;
        }

        return wait;
    }


}
