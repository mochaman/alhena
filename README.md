# Alhena 🌟

**Alhena** is an easy-to-use, multi-platform Gemini/Spartan browser sporting an uncluttered, modern interface. Browse the small web in style!

[![Alhena Screenshot](https://metaloupe.com/alhena/images/alhena_mac_main.png)](https://metaloupe.com/alhena/alhena.html)

🔗 **[Official Website](https://metaloupe.com/alhena/alhena.html)**

## Features

- Modern UI with dozens of themes
- Color emojis with four emoji sets
- ZWJ (Zero Width Joiner) emojis
- ANSI colors for formatted text
- Font chooser for proportional fonts
- Inline image viewer
- Smooth adaptive scrolling
- Asynchronous networking and file access
- Client certificate creation and management
- Trust On First Use (TOFU)
- Bookmarks and history
- Multiple windows and tabs
- Touch autoscrolling
- Backup data with merge
- Secure remote sync
- Titan upload protocol with ;edit
- HTTP and Gopher proxy support
- Built-In HTTP to Gemtext converter
- Spartan protocol

## 🎨 Design

The goal is to provide a familiar, easy-to-use interface for exploring Geminispace. Gemtext documents are rendered without added clutter. Emojis are displayed in color and ZWJ emojis appear as a single image. 👨🏼‍🚀

Any system font can be used for displaying proportional text. Dozens of light and dark themes are available to make browsing easy on the eyes. The interface is simple and intuitive.

All settings, bookmarks, certificates and history are saved in an embedded database that can be backed-up and restored on other computers. Secure remote sync can be used to easily move your settings to other machines.

## 🚀 Getting Started

### **Building from Source**
Alhena is built using **Maven** on Java 21 or greater. To compile, simply run:

```sh
git clone https://github.com/mochaman/alhena.git
cd alhena
mvn package
```

The `alhena.jar` will be created and copied to `target/lib`. To run, call java -jar with the path to /lib/alhena.jar. For example (if already in the target directory): 

```sh
java -jar ./lib/alhena.jar
```

## 📥 Installs

Prebuilt binaries are available for Windows, Mac, Linux and FreeBSD in Releases. Java is NOT required. Each archive includes a small, custom jvm created with jlink.

- [Windows x64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_windows_x64.zip) MSI installer
- [MacOS aarch64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_aarch64.dmg) DMG  (unsigned)
- [MacOS x64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_x64.dmg) DMG (unsigned)
- [Linux x64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_linux_x64.tgz) untar and run 'Alhena' script
- [Linux aarch64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_linux_aarch64.tgz) untar and run 'Alhena' script
- [FreeBSD x64](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_freebsd_x64.tgz) untar and run script
- [Standalone (No Java)](https://github.com/mochaman/alhena/releases/download/v5.0/alhena-5.0_nojava.zip) JAVA_HOME must point to Java 21+ directory. Unzip and run .bat or .sh.

## 🐳 Docker

You can try Alhena in a browser or VNC client with Docker.

🔗 **[Alhena on Docker Hub](https://hub.docker.com/r/bgrier1/alhena)**

```
docker run -p 6080:6080 -it --rm alhena
```
Then open your favorite browser and enter the following url:
```
http://localhost:6080/vnc.html
```
The password is `alhena`

## 📷 Screenshots

Titan Text Editor: 

![Dark Theme](https://metaloupe.com/alhena/images/titantexteditor.png)

Light Theme:

![Light Theme](https://metaloupe.com/alhena/images/windows_light.png)

ANSI Color:  

![Inline Images](https://metaloupe.com/alhena/images/mac_ansi.png)

## 🛠 License
Alhena is licensed under the **BSD 2-Clause License**. See [LICENSE](LICENSE) for details.

---

Have suggestions or found a bug? Open an issue or contribute via pull requests! 🚀


