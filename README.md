Haiku Player
===================

### Description

    Media Player built on top of VLC, VLCJ and various other open-source projects.

### Features

* Media player UI is built from ground up.
* Play media from across the web including YouTube and Vimeo. This is enabled by the
   open-source project YouTube-DL (https://github.com/rg3/youtube-dl).
* FileSystem Tree view to pin-point the library file being played on the computer.
    
### Releases
   Download the media player here:
   [releases](https://github.com/leogps/haiku/releases)

### Keyboard Shortcuts
#### Main Window
| Shortcut              | MacOS          | Windows        | Linux          |
|-----------------------|----------------|----------------|----------------|
| Open File             | `CMD` + O      | `CTRL` + O     | `CTRL` + O     |
| Open Network          | `CMD` + N      | `CTRL` + N     | `CTRL` + N     |
| Increase Volume       | `SHIFT` + UP   | `SHIFT` + UP   | `SHIFT` + UP   |
| Decrease Volume       | `SHIFT` + DOWN | `SHIFT` + DOWN | `SHIFT` + DOWN |
| Seek Position         | `CMD` + G      | `SHIFT` + G    | `SHIFT` + G    |
| Quit                  | `CMD` + Q      | `CTRL` + Q     | `CTRL` + Q     |
#### Player Window
| Shortcut             | MacOS            | Windows          | Linux            |
|----------------------|------------------|------------------|------------------|
| Toggle Fullscreen    | `CMD` + ENTER    | `CTRL` + ENTER   | `CTRL` + ENTER   |
| Mute/Un-Mute         | `CTRL` + M       | `CTRL` + M       | `CTRL` + M       |
| Increase Volume      | `SHIFT` + UP     | `SHIFT` + UP     | `SHIFT` + UP     |
| Decrease Volume      | `SHIFT` + DOWN   | `SHIFT` + DOWN   | `SHIFT` + DOWN   |
| Seek Forward         | `SHIFT` + RIGHT  | `SHIFT` + RIGHT  | `SHIFT` + RIGHT  |
| Seek Backwards       | `SHIFT` + LEFT   | `SHIFT` + LEFT   | `SHIFT` + LEFT   |
| Seek Forward(3sec)   | RIGHT            | RIGHT            | RIGHT            |
| Seek Backwards(3sec) | LEFT             | LEFT             | LEFT             |
| Quit                 | `CMD` + Q        | `CTRL` + Q       | `CTRL` + Q       |

### Executable
    1. While this can run on any machine with Java and VLC runtimes, this is primarily developed for Mac OSX and executables are compiled as Mac native app.
    2. Linux executable can be run using the java command.
    3. Windows executable: TBA 

### Development

#### Tools:    

*Note: Most developmental changes require simply running `mvn clean install`.    
Replacing the haiku-jar-with-dependencies.jar file from player-ui/target under the pre-built haiku.app/Contents/Java folder will suffice.
    
However, in order to update youtube-dl or to create the Mac OS X native app programmatically incorporating any changes, running `mvn clean install -Denvironment=dev` will do.
    
    Java
        Maven (3.3.x is tested to work)
    
YoutubeDL

    YoutubeDL project downloads executable for Linux/Mac OSX and also for Windows.

        
#### Dependent libraries

##### Apple JavaExtension
`mvn install:install-file -Dfile="lib-runtime/ui.jar" -DgroupId="com.apple" -DartifactId="AppleJavaExtensions" -Dversion="1.6" -Dpackaging="jar"`

##### VLC
    Mac OSX: Latest VLC runtime for Mac OSX is already including in the commits.
    For Linux, it is best to install the VLC which sets the LibVLC path automatically.  

##### Debian Archiver -- .deb file build (Optional)
Builds dpkg file that can be used with Debian systems. `dpkg` needs to be available for this to work. 

    cd archiver
    mvn clean install
    cd debian
    dpkg --build haiku


##### Build
After seeing that all non-optional requirements have been met, the project can be built using maven.
 
    mvn clean install
    
Or build without running tests.
    
    mvn clean install -DskipTests

---
       
Credits
---

##### VLC and its license can be found here:
http://www.videolan.org/legal.html

---

##### VLCJ library and its license can be found here:
https://github.com/caprica/vlcj

---

##### UI Designer, UI compilation, UI jar bundling powered by IntelliJ IDEA:
https://www.jetbrains.com/idea/
