package dev.harrel.jarhell.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FormatUtilTest {

    @ParameterizedTest
    @MethodSource("bytes")
    void formatsBytes(Long bytes, String formatted) {
        String actual = FormatUtil.formatBytes(bytes);
        assertThat(actual).isEqualTo(formatted);
    }

    @ParameterizedTest
    @MethodSource("bytecodeVersions")
    void formatsBytecodes(String bytecodeVersion, String formatted) {
        String actual = FormatUtil.formatBytecodeVersion(bytecodeVersion);
        assertThat(actual).isEqualTo(formatted);
    }

    static Stream<Arguments> bytes() {
        return Stream.of(
                Arguments.of(0L, "0B"),
                Arguments.of(1L, "1B"),
                Arguments.of(100L, "100B"),
                Arguments.of(999L, "999B"),
                Arguments.of(1_000L, "1.00KB"),
                Arguments.of(1_001L, "1.00KB"),
                Arguments.of(1_005L, "1.00KB"),
                Arguments.of(1_006L, "1.01KB"),
                Arguments.of(1_990L, "1.99KB"),
                Arguments.of(1_998L, "2.00KB"),
                Arguments.of(1_000_000L, "1.00MB"),
                Arguments.of(1_000_999L, "1.00MB"),
                Arguments.of(1_009_999L, "1.01MB"),
                Arguments.of(11_111_111L, "11.11MB"),
                Arguments.of(1_000_000_000L, "1.00GB"),
                Arguments.of(999_000_000_000L, "999.00GB"),
                Arguments.of(888_999_000_000_000L, "888999.00GB")
        );
    }

    static Stream<Arguments> bytecodeVersions() {
        return Stream.of(
                Arguments.of(null, "N/A"),
                Arguments.of("45.0", "java 1.0"),
                Arguments.of("52.0", "java 8"),
                Arguments.of("61.0", "java 17"),
                Arguments.of("61.65535", "java 17 (preview)"),
                Arguments.of("65.0", "java 21"),
                Arguments.of("65.65535", "java 21 (preview)"),
                Arguments.of("100.0", "java 56")
        );
    }
}