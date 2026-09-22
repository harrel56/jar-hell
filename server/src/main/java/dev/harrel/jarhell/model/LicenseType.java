package dev.harrel.jarhell.model;

import dev.harrel.jarhell.model.descriptor.License;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public enum LicenseType {
    APACHE_2(lowercaseSet(
            "Apache",
            "Apache Software",
            "Apache Software 2.0",
            "Apache Software 2",
            "Apache 2.0",
            "Apache 2",
            "Apache 2.0 January 2004",
            "AL 2.0",
            "AL 2",
            "AL2",
            "AL2.0",
            "ASL 2",
            "ASL 2.0",
            "ASL2",
            "ASL2.0"
    ), uris(
            "https://apache.org/licenses",
            "https://apache.org/licenses/LICENSE-2.0",
            "https://apache.org/licenses/LICENSE-2.0.txt",
            "https://apache.org/licenses/LICENSE-2.0.html",
            "https://opensource.org/licenses/Apache-2.0",
            "https://opensource.org/licenses/Apache-2-0",
            "https://opensource.org/licenses/apache-2.0",
            "https://opensource.org/licenses/apache-2-0",
            "https://repository.jboss.org/licenses/apache-2.0.txt"
    )),
    APACHE_1_1(lowercaseSet(
            "Apache 1.1",
            "Apache1.1",
            "Apache Software 1.1",
            "AL 1.1",
            "AL1.1",
            "ASL 1.1",
            "ASL1.1"
    ), uris(
            "https://apache.org/licenses/LICENSE-1.1",
            "https://apache.org/licenses/LICENSE-1.1.txt",
            "https://spdx.org/licenses/Apache-1.1.html"
    )),
    BSD_0(lowercaseSet(
            "0BSD",
            "BSD0",
            "BSD 0",
            "Zero Clause BSD",
            "BSD Zero Clause",
            "0 Clause BSD",
            "BSD 0 Clause"
    ), uris(
            "https://opensource.org/license/0bsd"
    )),
    BSD_1(lowercaseSet(
            "BSD1",
            "BSD 1",
            "1 Clause BSD",
            "BSD 1 Clause"
    ), uris(
            "https://opensource.org/license/bsd-1-clause"
    )),
    BSD_2(lowercaseSet(
            "Berkeley Software Distribution",
            "BSD",
            "BSD2",
            "BSD 2",
            "2 Clause BSD",
            "BSD 2 Clause",
            "Simplified BSD",
            "FreeBSD",
            "Free BSD"
    ), uris(
            "https://opensource.org/licenses/bsd-license.html",
            "https://opensource.org/licenses/bsd-license.php",
            "https://opensource.org/license/BSD-2-Clause"
    )),
    BSD_3(lowercaseSet(
            "BSD3",
            "BSD 3",
            "3 Clause BSD",
            "BSD 3 Clause",
            "BSDNew",
            "BSD New",
            "NewBSD",
            "New BSD",
            "Modified BSD",
            "Revised BSD"
    ), uris(
            "https://opensource.org/licenses/BSD-3-Clause"
    )),
    CC0_1(lowercaseSet(
            "CC0",
            "CC0 1",
            "CC0 1.0",
            "Creative Commons 1.0",
            "Creative Commons 1",
            "Creative Commons"
    ), uris(
            "https://repository.jboss.org/licenses/cc0-1.0.txt"
    )),
    CDDL_1(lowercaseSet(
            "CDDL",
            "CDDL1",
            "CDDL1.0",
            "CDDL 1",
            "CDDL 1.0",
            "CDDL 1.1",
            "CDDL1.1",
            "Common Development and Distribution 1.0",
            "Common Development and Distribution 1",
            "Common Development and Distribution 1.1",
            "Common Development and Distribution"
    ), uris(
            "https://opensource.org/license/cddl-1-0",
            "https://opensource.org/license/cddl1.txt",
            "https://repository.jboss.org/licenses/cddl.txt",
            "https://oss.oracle.com/licenses/CDDL-1.1",
            "https://spdx.org/licenses/CDDL-1.1.html"
    )),
    CPL_1(lowercaseSet(
            "CPL",
            "CPL1",
            "CPL1.0",
            "CPL 1",
            "Common Public 1.0",
            "Common Public 1",
            "Common Public"
    ), uris(
            "https://opensource.org/license/cpl1.0.txt"
    )),
    GPL_2(lowercaseSet(
            "GPL2",
            "GPL2.0",
            "GPL 2.0",
            "GPL 2",
            "GNU GPL 2.0",
            "GNU GPL 2",
            "GNU GPL2",
            "GNU General Public 2.0",
            "GNU General Public 2",
            "General Public 2.0",
            "General Public 2"
    ), uris(
            "https://opensource.org/license/gpl-2-0",
            "https://gnu.org/licenses/old-licenses/gpl-2.0.en.html"
    )),
    GPL_2_CPE(lowercaseSet(
            "GPL2 w/ CPE",
            "GPL2+CE",
            "GPL2 CE",
            "GPL 2 CE",
            "GPL 2.0 CE",
            "GPL 2 CPE",
            "GPL 2.0 CPE",
            "GPL 2 with classpath exception",
            "GPL 2.0 with classpath exception",
            "GPL2 with classpath exception",
            "GPL 2 with the classpath exception",
            "GPL 2.0 with the classpath exception",
            "GNU 2.0 with classpath exception",
            "GNU General Public 2 with classpath exception",
            "GNU General Public 2.0 with classpath exception",
            "GNU General Public 2 with gnu classpath exception",
            "GNU General Public 2.0 with gnu classpath exception",
            "GNU General Public 2.0 only with classpath exception",
            "CDDL+GPL",
            "CDDL+GPL2",
            "CDDL/GPL2+CE",
            "CDDL + GPL2 with classpath exception",
            "CDDL+GPL2 with classpath exception",
            "CDDL or GPL2 with exceptions",
            "Common Development and Distribution plus GPL",
            "Dual consisting of CDDL 1.1 and GPL 2"
    ), uris(
            "https://gnu.org/software/classpath/license.html",
            "https://openjdk.java.net/legal/gplv2+ce.html",
            "https://openjdk.org/legal/gplv2+ce.html",
            "https://repository.jboss.org/licenses/gpl-2.0-ce.txt",
            "https://projects.eclipse.org/license/secondary-gpl-2.0-cp",
            "https://spdx.org/licenses/GPL-2.0-with-classpath-exception.html"
    )),
    GPL_3(lowercaseSet(
            "GPL3",
            "GPL3.0",
            "GPL 3.0",
            "GPL 3",
            "GNU GPL 3.0",
            "GNU GPL 3",
            "GNU GPL3",
            "GNU General Public 3.0",
            "GNU General Public 3",
            "General Public 3.0",
            "General Public 3"
    ), uris(
            "https://opensource.org/license/gpl-3-0",
            "https://gnu.org/licenses/gpl-3.0.en.html"
    )),
    AGPL_3(lowercaseSet(
            "AGPL3",
            "AGPL3.0",
            "AGPL 3.0",
            "AGPL 3",
            "AGPL",
            "GNU AGPL 3.0",
            "GNU AGPL 3",
            "GNU AGPL3",
            "GNU AGPL",
            "Affero GNU General Public 3.0",
            "Affero GNU General Public 3",
            "Affero GNU General Public",
            "GNU Affero General Public 3.0",
            "GNU Affero General Public 3",
            "GNU Affero General Public"
    ), uris(
            "https://opensource.org/license/agpl-v3",
            "https://gnu.org/licenses/agpl-3.0.en.html"
    )),
    LGPL_2(lowercaseSet(
            "LGPL",
            "LGPL2",
            "LGPL2.1",
            "LGPL 2.1",
            "LGPL 2",
            "LGPL 2.1 or later",
            "LGPL 2.1+",
            "GNU LGPL",
            "GNU LGPL 2.1",
            "GNU LGPL 2",
            "GNU LGPL2",
            "Lesser General Public",
            "Lesser General Public 2.1",
            "Lesser General Public 2",
            "Lesser GNU Public 2.1",
            "GNU Lesser Public",
            "GNU Lesser General Public",
            "GNU Lesser General Public 2.1",
            "GNU Lesser General Public 2",
            "GNU Lesser General Public 2.1 or later",
            "GNU Library General Public",
            "GNU Library General Public 2.1",
            "GNU Library General Public 2.1 or later",
            "GNU Library or Lesser General Public 2.1"
    ), uris(
            "https://opensource.org/license/lgpl-2-1",
            "https://opensource.org/license/lgpl-2.1",
            "https://opensource.org/license/lgpl-license.php",
            "https://gnu.org/licenses/lgpl-2.1.html",
            "https://gnu.org/licenses/lgpl-2.1.txt",
            "https://gnu.org/licenses/old-licenses/lgpl-2.1.html",
            "https://gnu.org/licenses/old-licenses/lgpl-2.1.en.html",
            "https://gnu.org/licenses/old-licenses/lgpl-2.1.txt",
            "https://repository.jboss.org/licenses/lgpl-2.1.txt",
            "https://spdx.org/licenses/LGPL-2.1.html"
    )),
    LGPL_3(lowercaseSet(
            "LGPL3",
            "LGPL3.0",
            "LGPL 3.0",
            "LGPL 3",
            "LGPL 3.0 or later",
            "LGPL 3+",
            "GNU LGPL 3.0",
            "GNU LGPL 3",
            "GNU LGPL3",
            "GNU Lesser General Public 3.0",
            "GNU Lesser General Public 3",
            "GNU Lesser General Public 3.0 or later",
            "GNU General Lesser Public 3.0",
            "GNU General Lesser Public 3",
            "Lesser General Public 3.0",
            "Lesser General Public 3"
    ), uris(
            "https://opensource.org/license/lgpl-3-0",
            "https://gnu.org/licenses/lgpl-3.0.html",
            "https://gnu.org/licenses/lgpl-3.0.en.html",
            "https://gnu.org/licenses/lgpl-3.0.txt",
            "https://spdx.org/licenses/LGPL-3.0.html"
    )),
    MIT(lowercaseSet(
            "MIT",
            "Bouncy Castle" // the MIT text under a Bouncy Castle copyright line, SPDX lists it as MIT too
    ), uris(
            "https://opensource.org/license/mit",
            "https://opensource.org/license/MIT",
            "https://bouncycastle.org/licence.html",
            "https://bouncycastle.org/license.html"
    )),
    MIT0(lowercaseSet(
            "MIT0",
            "MIT 0",
            "MIT No Attribution"
    ), uris(
            "https://opensource.org/license/mit-0",
            "https://opensource.org/license/MIT-0"
    )),
    MPL_1(lowercaseSet(
            "Mozilla Public 1.1",
            "Mozilla Public 1",
            "MPL",
            "MPL1",
            "MPL1.1",
            "MPL 1",
            "MPL 1.1"
    ), uris(
            "https://opensource.org/license/mpl-1-1",
            "https://mozilla.org/en-US/MPL/1.1",
            "https://mozilla.org/MPL/MPL-1.1.html"
    )),
    MPL_2(lowercaseSet(
            "Mozilla Public 2.0",
            "Mozilla Public 2",
            "MPL",
            "MPL2",
            "MPL2.0",
            "MPL 2",
            "MPL 2.0"
    ), uris(
            "https://opensource.org/license/mpl-2-0",
            "https://mozilla.org/en-US/MPL/2.0"
    )),
    ISC(lowercaseSet(
            "ISC"
    ), uris(
            "https://opensource.org/license/isc-license-txt"
    )),
    ICU(lowercaseSet(
            "ICU"
    ), uris(
            "https://opensource.org/license/icu-license"
    )),
    EPL_1(lowercaseSet(
            "Eclipse Public",
            "Eclipse Public 1.0",
            "Eclipse Public 1",
            "EPL",
            "EPL1",
            "EPL1.0",
            "EPL 1.0",
            "EPL 1"
    ), uris(
            "https://opensource.org/license/epl-1-0",
            "https://eclipse.org/legal/epl/epl-v10.html"
    )),
    EPL_2(lowercaseSet(
            "Eclipse Public 2.0",
            "Eclipse Public 2",
            "EPL2",
            "EPL2.0",
            "EPL 2.0",
            "EPL 2"
    ), uris(
            "https://opensource.org/license/epl-2-0",
            "https://eclipse.org/legal/epl-2.0/"
    )),
    EDL_1(lowercaseSet(
            "EDL",
            "EDL1",
            "EDL1.0",
            "EDL 1",
            "EDL 1.0",
            "Eclipse Distribution",
            "Eclipse Distribution 1.0",
            "Eclipse Distribution 1"
    ), uris(
            "https://eclipse.org/org/documents/edl-v10.php",
            "https://eclipse.org/org/documents/edl-v10.html"
    )),
    SSPL_1(lowercaseSet(
            "SSPL",
            "SSPL1",
            "SSPL1.0",
            "SSPL 1",
            "SSPL 1.0",
            "Server Side Public 1.0",
            "Server Side Public 1",
            "Server Side Public"
    ), uris(
            "https://spdx.org/licenses/SSPL-1.0.html"
    )),
    UNLICENSE(lowercaseSet(
            "Unlicense",
            "Public Domain",
            "PD"
    ), uris(
            "https://opensource.org/license/unlicense"
    )),
    UPL_1(lowercaseSet(
            "UPL",
            "UPL 1.0",
            "UPL 1",
            "Universal Permissive",
            "Universal Permissive 1.0",
            "Universal Permissive 1"
    ), uris(
            "https://opensource.org/license/UPL",
            "https://oss.oracle.com/licenses/upl",
            "https://oracle.com/technetwork/licenses/upl-license-2927578.html",
            "https://spdx.org/licenses/UPL-1.0.html"
    )),
    ZLIB(lowercaseSet(
            "zlib",
            "zlib/libpng",
            "libpng",
            "lib png"
    ), uris(
            "https://opensource.org/license/zlib",
            "https://zlib.net/zlib_license.html"
    )),
    UNKNOWN(Set.of(), Set.of()),
    NO_LICENSE(Set.of(), Set.of());

    private static final Map<LicenseType, Integer> ORDER_MAP;
    static {
        Map<LicenseType, Integer> orderMap = new EnumMap<>(LicenseType.class);
        List<LicenseType> ordered = List.of(
                LicenseType.NO_LICENSE,
                LicenseType.UNKNOWN,
                LicenseType.SSPL_1,
                LicenseType.CC0_1, // controversial
                LicenseType.UNLICENSE, // controversial

                // copyleft
                LicenseType.AGPL_3,
                LicenseType.CDDL_1,
                LicenseType.GPL_2,
                LicenseType.GPL_3,

                // weak-copyleft
                LicenseType.GPL_2_CPE,
                LicenseType.LGPL_2,
                LicenseType.LGPL_3,
                LicenseType.CPL_1,
                LicenseType.EPL_1,
                LicenseType.EPL_2,
                LicenseType.MPL_1,
                LicenseType.MPL_2,

                // permissive
                LicenseType.BSD_1,
                LicenseType.APACHE_1_1,
                LicenseType.BSD_3,
                LicenseType.EDL_1,
                LicenseType.APACHE_2,
                LicenseType.BSD_2,
                LicenseType.ICU,
                LicenseType.ZLIB,
                LicenseType.ISC,
                LicenseType.MIT,
                LicenseType.UPL_1,
                LicenseType.BSD_0,
                LicenseType.MIT0
        );
        if (ordered.size() != LicenseType.values().length || !Set.copyOf(ordered).containsAll(Arrays.asList(LicenseType.values()))) {
            throw new IllegalStateException();
        }
        for (int i = 0; i < ordered.size(); i++) {
            orderMap.put(ordered.get(i), i);
        }
        ORDER_MAP = Collections.unmodifiableMap(orderMap);
    }

    // nested holder: enum constants are initialized before the enum's own static fields,
    // and uris() already needs URI_CLEANER while constructing them
    private static final class Cleaners {
        private static final Pattern NAME = Pattern.compile(
                ",|(\\bthe\\b)|(\\bversion\\b)|(\\blicense\\b)|(\\blicence\\b)|(\\bv\\b\\.?)|(v(?=\\d))|(\\.$)|(\\(.*\\))");
        private static final Pattern URI = Pattern.compile("((?<=//)www\\.)|(/$)");
    }
    public static final Comparator<LicenseType> COMPARATOR = Comparator.comparingInt(ORDER_MAP::get);

    private final Set<String> names;
    private final Set<URI> uris;

    LicenseType(Set<String> names, Set<URI> uris) {
        this.names = names;
        this.uris = uris;
    }

    public static LicenseType categorize(License license) {
        URI uri = normalizeUri(license.url());
        if (uri != null) {
            for (LicenseType type : LicenseType.values()) {
                if (type.uris.contains(uri)) {
                    return type;
                }
            }
        }
        String name = normalizeName(license.name());
        if (name != null) {
            for (LicenseType type : LicenseType.values()) {
                if (type.names.contains(name)) {
                    return type;
                }
            }
        }
        return UNKNOWN;
    }

    /// - lowercase
    /// - convert '-', '_' to spaces
    /// - get rid of 'v' if preceding a number or as single letter (including a trailing dot, 'v. 2.0')
    /// - remove 'the', 'version', 'license'
    /// - remove commas
    /// - remove trailing periods
    /// - remove all content inside '()'
    /// - StringUtils.normalizeSpace() - to dedup spaces
    /// - trim()
    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        name = name.toLowerCase()
                .replace('-', ' ')
                .replace('_', ' ');
        name = Cleaners.NAME.matcher(name).replaceAll("");
        return StringUtils.normalizeSpace(name).trim();
    }

    /// - lowercase (paths are case-sensitive in theory, in practice license URLs are not)
    /// - http -> https
    /// - remove leading 'www'
    /// - remove trailing slash
    /// - opensource.org/licenses/ -> opensource.org/license/ (both forms are in use)
    private static URI normalizeUri(String uriString) {
        if (uriString == null) {
            return null;
        }
        uriString = uriString.toLowerCase()
                .replace("http://", "https://")
                .replace("opensource.org/licenses/", "opensource.org/license/");
        uriString = Cleaners.URI.matcher(uriString).replaceAll("");
        try {
            return new URI(uriString);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static Set<String> lowercaseSet(String... items) {
        return Arrays.stream(items).map(String::toLowerCase).collect(Collectors.toUnmodifiableSet());
    }

    private static Set<URI> uris(String... items) {
        return Arrays.stream(items).map(LicenseType::normalizeUri).collect(Collectors.toUnmodifiableSet());
    }
}
