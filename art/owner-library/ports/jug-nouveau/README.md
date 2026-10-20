# Jug Nouveau archive cross-check — 18 October 2026

The owner requested direct integration of their modified Ars collection. In
addition to the original Magic upload, two local archives were found:

| Owner archive | Resource entries | Already preserved exactly | Additional preserved variants |
|---|---:|---:|---:|
| `ars_jimbaux-1.21.1-5.13.3.jar` | 3,753 | 3,753 | 0 |
| `ars_jymbaumental-1.21.1-0.7.10.4.jar` | 1,094 | 1,066 | 28 |

All **4,847 resource entries** across both archives now have an exact preserved
source. This counts entries in each archive separately; shared paths may occur
in both. The core `ars_jimbaux` namespace accounts for 3,607 of them. The other
entries include cross-namespace tags and integration data.

The 28 elemental variants are mostly shared tags. Their bytes differ from the
file at the same path in the original combined Magic upload. Both versions are
kept: the combined upload is untouched, and the archive-specific version is
under `ars_jymbaumental-1.21.1-0.7.10.4/`. Future ports must deliberately reconcile
these tags rather than silently overwrite one mod's input with another's.

- [Every resource path, source archive, preserved destination and SHA-256](resources.csv)
- [Archive hashes, counts and compatibility](summary.json)
- [Core archive metadata and original notice](ars_jimbaux-1.21.1-5.13.3/META-INF)
- [Elemental archive metadata](ars_jymbaumental-1.21.1-0.7.10.4/META-INF)
- [Implementation and remaining port families](../../../../docs/features/jug-nouveau.md)

The archives contain compiled classes (including embedded dependencies), but no
Java/Kotlin source project. Their metadata targets NeoForge; Jugcraft uses
Fabric 26.3. They were inspected as ZIP data and, for the Book Wyrm API, with JDK
`javap`; no archive code was executed. Original metadata/notices are preserved,
including previous project names. Those labels alone do not invalidate the
owner's standing asset authorization.

Compiled classes and nested dependencies are not copied into Jugcraft, and the
old loaders, mixins and foreign recipe serializers are not activated. Resources
are available for direct reuse; functioning gameplay still needs compatible
Jugcraft code. Source preservation is not a claim of runtime integration or
feature parity.

Reproduce with `python -X utf8 scripts/audit_jug_nouveau_archives.py` followed by
the two archive paths, core first. The script reads ZIP entries, hashes them,
preserves nonmatching resource variants and records metadata; it never starts
the JARs. The original [catalog](../../catalog/README.md) remains the catalog of
the original uploads; this separate CSV records the archive cross-check and
supplementary variants.
