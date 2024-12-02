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
            "Apache Software 2.0",
            "Apache Software 2",
            "Apache 2.0",
            "Apache 2",
            "AL 2.0",
            "AL 2",
            "AL2",
            "AL2.0"
    ), uris(
            "https://apache.org/licenses/LICENSE-2.0",
            "https://apache.org/licenses/LICENSE-2.0.txt",
            "https://apache.org/licenses/LICENSE-2.0.html",
            "https://opensource.org/licenses/Apache-2.0",
            "https://opensource.org/licenses/Apache-2-0",
            "https://opensource.org/licenses/apache-2.0",
            "https://opensource.org/licenses/apache-2-0",
            "https://repository.jboss.org/licenses/apache-2.0.txt"
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
            "Common Development and Distribution 1.0",
            "Common Development and Distribution 1",
            "Common Development and Distribution"
    ), uris(
            "https://opensource.org/license/cddl-1-0",
            "https://repository.jboss.org/licenses/cddl.txt"
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
            "LGPL2",
            "LGPL2.1",
            "LGPL 2.1",
            "LGPL 2",
            "GNU LGPL 2.1",
            "GNU LGPL 2",
            "GNU LGPL2",
            "GNU Lesser General Public 2.1",
            "GNU Lesser General Public 2",
            "Lesser General Public 2.1",
            "Lesser General Public 2"
    ), uris(
            "https://opensource.org/license/lgpl-2-1",
            "https://gnu.org/licenses/old-licenses/lgpl-2.1.en.html"
    )),
    LGPL_3(lowercaseSet(
            "LGPL3",
            "LGPL3.0",
            "LGPL 3.0",
            "LGPL 3",
            "GNU LGPL 3.0",
            "GNU LGPL 3",
            "GNU LGPL3",
            "GNU Lesser General Public 3.0",
            "GNU Lesser General Public 3",
            "Lesser General Public 3.0",
            "Lesser General Public 3"
    ), uris(
            "https://opensource.org/license/lgpl-3-0",
            "https://gnu.org/licenses/lgpl-3.0.en.html"
    )),
    MIT(lowercaseSet(
            "MIT"
    ), uris(
            "https://opensource.org/license/mit",
            "https://opensource.org/license/MIT"
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
            "https://mozilla.org/en-US/MPL/1.1"
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
                LicenseType.CC0_1,
                LicenseType.UNLICENSE,
                LicenseType.AGPL_3,
                LicenseType.CDDL_1,
                LicenseType.GPL_2,
                LicenseType.GPL_3,
                LicenseType.LGPL_2,
                LicenseType.LGPL_3,
                LicenseType.CPL_1,
                LicenseType.EPL_1,
                LicenseType.EPL_2,
                LicenseType.MPL_1,
                LicenseType.MPL_2,
                LicenseType.BSD_1,
                LicenseType.BSD_3,
                LicenseType.APACHE_2,
                LicenseType.BSD_2,
                LicenseType.ICU,
                LicenseType.ZLIB,
                LicenseType.ISC,
                LicenseType.MIT,
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

    private static final Pattern NAME_CLEANER = Pattern.compile(
            ",|(\\bthe\\b)|(\\bversion\\b)|(\\blicense\\b)|(\\blicence\\b)|(\\bv\\b)|(v(?=\\d))|(\\.$)|(\\(.*\\))");
    private static final Pattern URI_CLEANER = Pattern.compile("((?<=//)www\\.)|(/$)");
    public static final Comparator<LicenseType> COMPARATOR = Comparator.comparingInt(ORDER_MAP::get);

    private final Set<String> names;
    private final Set<URI> uris;

    LicenseType(Set<String> names, Set<URI> uris) {
        this.names = names;
        this.uris = uris;
    }

    public static LicenseType categorize(License license) {
        URI uri = normalizeUri(license.url());
        String name = normalizeName(license.name());
        for (LicenseType type : LicenseType.values()) {
            if ((uri != null && type.uris.contains(uri)) || (name != null && type.names.contains(name))) {
                return type;
            }
        }
        return UNKNOWN;
    }

    /// - lowercase
    /// - convert '-', '_' to spaces
    /// - get rid of 'v' if preceding a number or as single letter
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
        name = StringUtils.truncate(name, 64)
                .toLowerCase()
                .replace('-', ' ')
                .replace('_', ' ');
        name = NAME_CLEANER.matcher(name).replaceAll("");
        return StringUtils.normalizeSpace(name).trim();
    }

    /// - http -> https
    /// - remove leading 'www'
    /// - remove trailing slash
    private static URI normalizeUri(String uriString) {
        if (uriString == null) {
            return null;
        }
        uriString = StringUtils.truncate(uriString, 64)
                .replace("http://", "https://");
        uriString = URI_CLEANER.matcher(uriString).replaceAll("");
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
        return Arrays.stream(items).map(URI::create).collect(Collectors.toUnmodifiableSet());
    }
}
