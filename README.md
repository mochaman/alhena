# Alhena 🌟

**Alhena** is a full-featured, multi-platform Gemini browser with a modern UI, asynchronous networking, and certificate management.

[![Alhena Screenshot](https://metaloupe.com/alhena/alhena4.png)](https://metaloupe.com/alhena/alhena.html)

🔗 **[Official Website & More Info](https://metaloupe.com/alhena/alhena.html)**

## Features

- Modern UI with multiple themes
- Color emojis on Mac OS
- Inline image viewer
- Asynchronous networking and file access
- Client certificate management
- Bookmarks and history
- Multiple windows and tabs

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

Prebuilt binaries are available for various platforms. Java is NOT required. Each archive includes a small, custom jvm created with jlink.



| Platform            | Type  | Download Link |
|---------------------|------|--------------|
| **Windows x64**     | MSI  | [Download](https://metaloupe.com/alhena/alhena-1.6_windows_x64.zip) |
| **Mac x64 (Intel)** | DMG  | [Download](https://metaloupe.com/alhena/alhena-1.6_x64.dmg) |
| **Mac AArch64 (M1/M2)** | DMG  | [Download](https://metaloupe.com/alhena/alhena-1.6_aarch64.dmg) |
| **Linux x64**       | TGZ  | [Download](https://metaloupe.com/alhena/alhena-1.6_linux_x64.tgz) |
| **Linux AArch64 (Pi, etc)** | TGZ  | [Download](https://metaloupe.com/alhena/alhena-1.6_linux_aarch64.tgz) |
| **FreeBSD x64**     | TGZ  | [Download](https://metaloupe.com/alhena/alhena-1.6_freebsd_x64.tgz) |


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
![Dark Theme](https://metaloupe.com/alhena/alhena1.png)

Light Theme:  
![Light Theme](https://metaloupe.com/alhena/alhena2.png)

Inline Images:  
![Inline Images](https://metaloupe.com/alhena/alhena3.png)

## 🛠 License
Alhena is licensed under the **BSD 2-Clause License**. See [LICENSE](LICENSE) for details.

---

Have suggestions or found a bug? Open an issue or contribute via pull requests! 🚀


