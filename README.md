# Android Mugen

This project is a fork of [DosBox Turbo](https://sites.google.com/site/dosboxturbo/), modified to run Mugen games through of DosMugen.

## Build

### Dependencies

- Linux;
- Java;
- Android SDK;
- Android NDK r21;
- DosMugen;
- csdpmi*b.zip.

### Compile project

1. Copy your DosMugen game to directory **app/src/main/assets/mugen**;
2. Extract **csdpmi*b.zip** on **app/src/main/assets/mugen**;
3. Execute `./gradlew build` on root directory of this project.

## Controls

The game can be controlled by **touch**, **keyboard** and **joystick**.

### Configure

To allow the game works with **touch** and **joystick**, you need configure the game controls on file `/data/mugen.cfg` of M.U.G.E.N., setting the **player 1** keys like:

```
[P1 Keys]
Jump   = 17
Crouch = 31
Left   = 30
Right  = 32
A      = 36
B      = 37
C      = 38
X      = 22
Y      = 23
Z      = 24
Start  = 28
```

This configuration set the follow keys for **player 1**:

- `w` - UP
- `r` - RIGHT
- `s` - DOWN
- `a` - LEFT
- `j` - A
- `k` - B
- `l` - C
- `u` - X
- `i` - Y
- `o` - Z
- `F1` - F1
- `Enter` - Start
