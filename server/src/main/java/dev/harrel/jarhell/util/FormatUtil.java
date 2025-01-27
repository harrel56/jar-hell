package dev.harrel.jarhell.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FormatUtil {
    private final static BigDecimal THOUSAND = new BigDecimal(1_000);
    private final static BigDecimal MILLION = new BigDecimal(1_000_000);
    private final static BigDecimal BILLION = new BigDecimal(1_000_000_000);

    public static String formatBytes(Long bytes) {
        if (bytes < 1_000) {
            return bytes + "B";
        }
        BigDecimal bytesDecimal = new BigDecimal(bytes).setScale(2, RoundingMode.HALF_EVEN);
        if (bytes < 1_000_000) {
            return bytesDecimal.divide(THOUSAND, RoundingMode.HALF_EVEN) + "KB";
        }
        if (bytes < 1_000_000_000) {
            return bytesDecimal.divide(MILLION, RoundingMode.HALF_EVEN) + "MB";
        } else {
            return bytesDecimal.divide(BILLION, RoundingMode.HALF_EVEN) + "GB";
        }
    }

    public static String formatBytecodeVersion(String bytecode) {
        return switch (bytecode) {
            case null -> "N/A";
            case "45.0" -> "java 1.0";
            case "45.3" -> "java 1.1";
            case "46.0" -> "java 1.2";
            case "47.0" -> "java 1.3";
            case "48.0" -> "java 1.4";
            default -> {
                String[] split = bytecode.split("\\.");
                int version = Integer.parseInt(split[0]) - 44;
                boolean preview = "65535".equals(split[1]);
                yield "java " + version + (preview ? " (preview)" : "");
            }
        };
    }
}
