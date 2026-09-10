# Open source and licensing policy

## Goal

Use open-source libraries selectively where they provide real leverage, while avoiding dependency sprawl and accidental licensing/model restrictions in a commercial consumer app.

## Default license posture

### Green

Generally acceptable subject to normal notice requirements:

- Apache-2.0
- MIT
- BSD family

### Review required

- LGPL
- custom source-available licenses
- model-specific licenses
- hosted SDK terms with unusual data restrictions

### Block unless explicitly approved

- GPL components linked/distributed in ways incompatible with product distribution
- AGPL server components unless deliberate compliance is acceptable
- unclear/non-commercial model licenses

## AI model distinction

Open repository != unrestricted model.

For each AI/audio dependency review separately:

```text
code license
model weights license
commercial use
redistribution
hosted use
training/data terms
```

LiveKit-related turn detection/models were specifically flagged during discovery as an example where model license may differ from core project license.

## Current experimental RTC dependency

M8 currently pins:

```text
io.github.webrtc-sdk:android:150.7871.01
```

The Maven artifact declares the 3-Clause BSD License, which fits the current green license posture.

It is intentionally included through `debugImplementation` only. It is an M8 engineering-spike dependency, not yet an approved production/release dependency. Promotion to production would still require dependency/vulnerability review, binary-size review, notices, physical-device evidence and an M8 architecture decision.

## Weather

Open-Meteo was considered for prototypes. Its data/server/commercial terms must be reviewed before production use.

## Dependency inventory

Maintain a generated dependency/SBOM inventory before beta.

Recommended CI checks:

- dependency license scan
- known vulnerability scan
- Gradle dependency analysis
- backend package audit

## Avoid dependency sprawl

Do not add a library for abstractions that are simple to own, especially:

- wake state machine
- basic MVI wrapper
- tiny policy/value objects

The critical alarm path should have especially conservative dependency count.
