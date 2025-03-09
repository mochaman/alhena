# Alhena 🌟

**Alhena** is a full-featured, multi-platform Gemini browser with a modern UI, asynchronous networking, and certificate management. Browse the small web in style!

[![Alhena Screenshot](https://metaloupe.com/alhena/images/alhena_main.png)](https://metaloupe.com/alhena/alhena.html)

🔗 **[Official Website & More Info](https://metaloupe.com/alhena/alhena.html)**

## Features

- Modern UI with multiple themes
- Color emojis on all platforms
- ANSI colors for formatted text
- Font chooser for proportional fonts
- Inline image viewer
- Smooth adaptive scrolling
- Asynchronous networking and file access
- Client certificate creation and management
- Bookmarks and history
- Multiple windows and tabs
- Touch autoscrolling
- Backup data with merge
- Remote sync with Replace/Merge options
- Titan upload protocol

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

## 📥 Downloads

Prebuilt binaries are available for Windows, Mac, Linux and FreeBSD in Releases. Java is NOT required. Each archive includes a small, custom jvm created with jlink.

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

Dark Theme:  
![Dark Theme](https://metaloupe.com/alhena/images/alhena_dark.png)

Light Theme:  
![Light Theme](https://metaloupe.com/alhena/images/alhena_light.png)

Inline Images:  
![Inline Images](https://metaloupe.com/alhena/images/alhena_inline.png)

## 🛠 License
Alhena is licensed under the **BSD 2-Clause License**. See [LICENSE](LICENSE) for details.

---

Have suggestions or found a bug? Open an issue or contribute via pull requests! 🚀
