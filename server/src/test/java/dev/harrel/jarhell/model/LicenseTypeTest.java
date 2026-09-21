package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.License;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseTypeTest {
    @ParameterizedTest
    @MethodSource("licenses")
    void shouldCategorizeCorrectly(License license, LicenseType licenseType) {
        LicenseType actualType = LicenseType.categorize(license);
        assertThat(actualType).isEqualTo(licenseType);
    }

    @Test
    void shouldSortByRestrictiveness() {
        List<LicenseType> sorted = Stream.of(
                        LicenseType.NO_LICENSE,
                        LicenseType.APACHE_2,
                        LicenseType.AGPL_3,
                        LicenseType.NO_LICENSE,
                        LicenseType.UNKNOWN,
                        LicenseType.CC0_1,
                        LicenseType.MIT
                )
                .sorted(LicenseType.COMPARATOR)
                .toList();

        assertThat(sorted).containsExactly(
                LicenseType.NO_LICENSE,
                LicenseType.NO_LICENSE,
                LicenseType.UNKNOWN,
                LicenseType.CC0_1,
                LicenseType.AGPL_3,
                LicenseType.APACHE_2,
                LicenseType.MIT
        );
    }

    @Test
    void shouldFindMostRestrictiveByMin() {
        LicenseType min = Stream.of(
                        LicenseType.APACHE_2,
                        LicenseType.GPL_3,
                        LicenseType.MPL_1,
                        LicenseType.MPL_2,
                        LicenseType.LGPL_3,
                        LicenseType.MIT
                )
                .min(LicenseType.COMPARATOR)
                .orElseThrow();

        assertThat(min).isEqualTo(LicenseType.GPL_3);
    }

    private static Stream<Arguments> licenses() {
        return Stream.of(
                Arguments.of(new License(null, null), LicenseType.UNKNOWN),
                Arguments.of(new License("Apache 2.0", null), LicenseType.APACHE_2),
                Arguments.of(new License("The Apache Software License 2", null), LicenseType.APACHE_2),
                Arguments.of(new License("ALv2.0", null), LicenseType.APACHE_2),
                Arguments.of(new License("BSD-0", null), LicenseType.BSD_0),
                Arguments.of(new License("0BSD", null), LicenseType.BSD_0),
                Arguments.of(new License("bsd-1-clause", null), LicenseType.BSD_1),
                Arguments.of(new License("bsd_2_clause", null), LicenseType.BSD_2),
                Arguments.of(new License("BSD", null), LicenseType.BSD_2),
                Arguments.of(new License("free_BSD", null), LicenseType.BSD_2),
                Arguments.of(new License("revised bsd", null), LicenseType.BSD_3),
                Arguments.of(new License("Common Development and Distribution, Version v1.0", null), LicenseType.CDDL_1),
                Arguments.of(new License("Common Public License.", null), LicenseType.CPL_1),
                Arguments.of(new License("  mit ", null), LicenseType.MIT),
                Arguments.of(new License("Eclipse Public Licence, v. 2.0", null), LicenseType.EPL_2),
                Arguments.of(new License("Eclipse Public License v. 2.0", null), LicenseType.EPL_2),
                Arguments.of(new License("Eclipse Public License - v. 1.0", null), LicenseType.EPL_1),
                Arguments.of(new License("Mozilla Public Licence, v 2.0", null), LicenseType.MPL_2),
                Arguments.of(new License("EPLv1", null), LicenseType.EPL_1),
                Arguments.of(new License("EPLv2.0", null), LicenseType.EPL_2),
                Arguments.of(new License("AGPL v3", null), LicenseType.AGPL_3),
                Arguments.of(new License("The Unlicense", null), LicenseType.UNLICENSE),
                Arguments.of(new License("BSD 5.0", null), LicenseType.UNKNOWN),
                Arguments.of(new License("", null), LicenseType.UNKNOWN),
                Arguments.of(new License("???????????????????????????????????????????????", null), LicenseType.UNKNOWN),

                Arguments.of(new License(null, "https://www.apache.org/licenses/LICENSE-2.0"), LicenseType.APACHE_2),
                Arguments.of(new License(null, "https://opensource.org/license/bsd-1-clause"), LicenseType.BSD_1),
                Arguments.of(new License(null, "http://www.opensource.org/license/bsd-1-clause"), LicenseType.BSD_1),
                Arguments.of(new License(null, "http://opensource.org/license/bsd-1-clause"), LicenseType.BSD_1),
                Arguments.of(new License(null, "http://opensource.org/license/bsd-1-clause/"), LicenseType.BSD_1),
                Arguments.of(new License(null, "https://opensource.org/licenses/bsd-1-clause"), LicenseType.BSD_1),
                Arguments.of(new License(null, "https://opensource.org/licenses/BSD-1-Clause"), LicenseType.BSD_1),
                Arguments.of(new License(null, "HTTPS://WWW.APACHE.ORG/LICENSES/LICENSE-2.0.TXT"), LicenseType.APACHE_2),
                Arguments.of(new License(null, "http://opensource.org/licenses/UPL"), LicenseType.UPL_1),
                Arguments.of(new License(null, "https://oss.oracle.com/licenses/upl"), LicenseType.UPL_1),
                Arguments.of(new License("Universal Permissive License, Version 1.0", null), LicenseType.UPL_1),
                Arguments.of(new License("GPL2 w/ CPE", null), LicenseType.GPL_2_CPE),
                Arguments.of(new License("GPL-2.0-with-classpath-exception", null), LicenseType.GPL_2_CPE),
                Arguments.of(new License("The GNU General Public License (GPL), Version 2, With Classpath Exception", null), LicenseType.GPL_2_CPE),
                Arguments.of(new License("GNU General Public License, version 2 with the GNU Classpath Exception", null), LicenseType.GPL_2_CPE),
                Arguments.of(new License("CDDL + GPLv2 with classpath exception", null), LicenseType.GPL_2_CPE),
                Arguments.of(new License(null, "https://www.gnu.org/software/classpath/license.html"), LicenseType.GPL_2_CPE),
                Arguments.of(new License(null, "http://openjdk.java.net/legal/gplv2+ce.html"), LicenseType.GPL_2_CPE),
                // one pom, two entries sharing the dual-license url (listed under no type): the name decides which half each one is
                Arguments.of(new License("CDDL 1.1", "https://glassfish.java.net/public/CDDL+GPL_1_1.html"), LicenseType.CDDL_1),
                Arguments.of(new License("CDDL 1.0", "https://glassfish.dev.java.net/public/CDDL+GPL.html"), LicenseType.CDDL_1),
                Arguments.of(new License("unknown name", "https://glassfish.dev.java.net/public/CDDL+GPL.html"), LicenseType.UNKNOWN),
                // a versioned url beats an unversioned name, whatever the declaration order
                Arguments.of(new License("BSD", "https://opensource.org/licenses/BSD-3-Clause"), LicenseType.BSD_3),
                Arguments.of(new License("GPL2 w/ CPE", "https://glassfish.java.net/public/CDDL+GPL_1_1.html"), LicenseType.GPL_2_CPE),
                Arguments.of(new License("Common Development and Distribution License (CDDL), Version 1.1", "https://oss.oracle.com/licenses/CDDL-1.1"), LicenseType.CDDL_1),
                // unversioned lgpl means 2.1, a versioned name decides when the url carries no version
                Arguments.of(new License("LGPL", null), LicenseType.LGPL_2),
                Arguments.of(new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/lgpl.html"), LicenseType.LGPL_2),
                Arguments.of(new License("GNU LESSER GENERAL PUBLIC LICENSE", "http://www.gnu.org/licenses/lgpl.txt"), LicenseType.LGPL_2),
                Arguments.of(new License("GNU Library General Public License v2.1 or later", "http://www.opensource.org/licenses/LGPL-2.1"), LicenseType.LGPL_2),
                Arguments.of(new License("Lesser Gnu Public License (LGPL), Version 2.1", null), LicenseType.LGPL_2),
                Arguments.of(new License(null, "http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html"), LicenseType.LGPL_2),
                Arguments.of(new License("GNU General Lesser Public License (LGPL) version 3.0", "http://www.gnu.org/licenses/lgpl.html"), LicenseType.LGPL_3),
                Arguments.of(new License("GNU Lesser General Public License", "http://www.gnu.org/licenses/lgpl-3.0.txt"), LicenseType.LGPL_3),
                // plain GPL 2 must not be pulled into the classpath-exception bucket
                Arguments.of(new License("GNU General Public License, version 2", null), LicenseType.GPL_2),
                Arguments.of(new License("The Universal Permissive License (UPL), Version 1.0", null), LicenseType.UPL_1),
                Arguments.of(new License(null, "sftp://opensource.org/license/bsd-1-clause/"), LicenseType.UNKNOWN),
                Arguments.of(new License(null, "is it uri?"), LicenseType.UNKNOWN),

                Arguments.of(new License("My License", "https://github.com"), LicenseType.UNKNOWN),
                Arguments.of(new License("GPL 3.0", "https://github.com"), LicenseType.GPL_3),
                Arguments.of(new License("GPL 3.0", "https://www.apache.org/licenses/LICENSE-2.0"), LicenseType.APACHE_2)
        );
    }
}