# Jar Hell ![jarhell](web/static/jarhell.png)

https://jarhell.harrel.dev

JVM packages analyzer. One place to check for:
- total package **size** (including its dependencies),
- total **number of dependencies**,
- compatible **Java version** (taking into account bytecode of dependencies as well),
- effective **license** of a package (even just one little dependency which has no license can make the whole library unusable).

## Badges

Badges are based on https://shield.io API. Style of each badge can be overridden by providing query parameters supported by `shields.io`. There are 6 types of badges available:
1. Package size (without dependencies) [![build](https://jarhell.harrel.dev/api/v1/badges/size/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/size/{gav}`.
2. Total package size (with dependencies) [![build](https://jarhell.harrel.dev/api/v1/badges/total_size/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/total_size/{gav}`.
3. Bytecode version (without dependencies) [![build](https://jarhell.harrel.dev/api/v1/badges/bytecode/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/bytecode/{gav}`.
4. Effective bytecode version (with dependencies) [![build](https://jarhell.harrel.dev/api/v1/badges/effective_bytecode/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/effective_bytecode/{gav}`. 
5. Number of required dependencies [![build](https://jarhell.harrel.dev/api/v1/badges/dependencies/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/dependencies/{gav}`.
6. Number of optional dependencies [![build](https://jarhell.harrel.dev/api/v1/badges/optional_dependencies/dev.harrel:json-schema)](https://jarhell.harrel.dev/packages/dev.harrel:json-schema) `https://jarhell.harrel.dev/api/v1/badges/optional_dependencies/{gav}`

Where `{gav}` should be coordinates of your package in `groupId:artifactId:version` notation. Passing no version will fall back to the latest one. 

## Contributing

Any bug fixes or ideas for new features (like a new metrics) are welcome.
