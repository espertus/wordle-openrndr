# Wordle

This is a clone of Wordle using [OPENRNDR](https://openrndr.org/).

## Game Instructions

Play is similar to the original Wordle, except:

* You can exit the game by pressing the escape key.
* Once the game ends, you can start a new one by pressing any key besides escape.

## Build instructions
### Gradle tasks
 - `run` runs the game
 - `jar` creates an executable platform-specific jar file with all dependencies
 - `zipDistribution` creates a zip file containing the application jar and the data folder
 - `jpackageZip` creates a zip with a stand-alone executable for the current platform (works with Java 14 only)

### Cross builds
To create runnable jars for a platform different from the platform you use to build one uses `./gradlew jar --PtargetPlatform=<platform>`. The supported platforms are `windows`, `macos`, `linux-x64` and `linux-arm64`. 

## Credits

This is based on the [OPENRNDR template project](https://github.com/openrndr/openrndr-template)
and relies on [orx-gui](https://github.com/openrndr/orx/tree/master/orx-jvm/orx-gui).

I received help from kind and generous people on the OPENRNDR Slack server,
especially [aBe](https://hamoid.com/).

The source of [secret-words.txt](data/dictionaries/secret-words.txt) is
[Here Lies Wordle: Full Answer List (NYT Update)](https://medium.com/@owenyin/here-lies-wordle-2021-2027-full-answer-list-52017ee99e86) 
by Owen Yin.

The source of [legal-words.txt](data/dictionaries/legal-words.txt) was
[https://raw.githubusercontent.com/tabatkins/wordle-list/main/words](https://raw.githubusercontent.com/tabatkins/wordle-list/main/words),
maintained by [Tab Atkins Jr.](https://xanthir.com/)

## Contributions

I welcome pull requests for more experienced OPENRNDR programmers.
