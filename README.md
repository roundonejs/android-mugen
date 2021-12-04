# Android Mugen

This project is a fork of [DosBox Turbo](https://sites.google.com/site/dosboxturbo/), modified to run Mugen games through of DosMugen.

## Build

### Dependencies

- Linux;
- Java;
- Android SDK;
- Android NDK r9;
- DosMugen;
- csdpmi*b.zip.

### Environment variables

- `NDK_PATH` the path for **Android NDK r9** directory.

### Compile project

1. Copy your DosMugen game to directory **app/src/main/assets/mugen**;
2. Extract **csdpmi*b.zip** on **app/src/main/assets/mugen**;
3. Execute `./gradlew build` on root directory of this project.
