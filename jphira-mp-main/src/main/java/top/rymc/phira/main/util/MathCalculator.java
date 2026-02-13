package top.rymc.phira.main.util;

import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathCalculator {

    private static final MathContext DEFAULT_MC = new MathContext(256, RoundingMode.HALF_UP);

    public static BigDecimal accScore(double p, double q) {
        return calculate(p, 10, 150, q, 2500, 500);
    }

    public static BigDecimal stdScore(double p, double q) {
        return calculate(p, 10, - (7.0/3.0), q, 2500, 500);
    }

    public static BigDecimal calculate(double p, double a, double b, double q, double c, double d) {
        return calculate(p, a, b, q, c, d, DEFAULT_MC);
    }

    public static BigDecimal calculate(double p, double a, double b, double q, double c, double d, MathContext mc) {

        BigDecimal bdP = BigDecimal.valueOf(p);
        BigDecimal bdA = BigDecimal.valueOf(a);
        BigDecimal bdB = BigDecimal.valueOf(b);
        BigDecimal bdQ = BigDecimal.valueOf(q);
        BigDecimal bdC = BigDecimal.valueOf(c);
        BigDecimal bdD = BigDecimal.valueOf(d);

        BigDecimal bq = bdB.multiply(bdQ, mc);

        BigDecimal numerator = bq.add(bdC, mc);

        BigDecimal exponent = numerator.divide(bdD, mc);

        BigDecimal powerResult = BigDecimalMath.pow(bdA, exponent, mc);

        return bdP.multiply(powerResult, mc);
    }

}
