# UTI Score

Aplicativo Android nativo para calculadoras de UTI e geracao de justificativa clinica.

## Escores incluidos

- SOFA
- APACHE II
- Glasgow Coma Scale
- CURB-65
- Wells para TEP
- qSOFA
- CHA2DS2-VASc
- HAS-BLED
- Child-Pugh
- TIMI UA/NSTEMI
- SAPS 3
- NEWS2
- MEWS
- KDIGO para lesao renal aguda
- Relacao P/F e indice de oxigenacao
- sPESI
- RASS
- Braden
- Criterios Sepsis-3 para sepse e choque septico

## Build

```bash
./gradlew assembleDebug
```

O APK de debug e gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## iOS

Ha tambem uma versao iOS nativa em SwiftUI:

```text
ios/UTIScore.xcodeproj
```

Para abrir no Xcode:

```bash
open ios/UTIScore.xcodeproj
```

No Xcode, selecione o target `UTIScore`, escolha o Team em `Signing & Capabilities` e rode no simulador ou iPhone.
