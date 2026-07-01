# Distributing to Linux App Stores

This guide explains how to use the generated configuration files to publish Quran Desktop to the Snap Store and Flathub.

## 1. Publishing to the Snap Store

The configuration file is located at `snap/snapcraft.yaml`.

### Prerequisites
1.  Register an account at [Snapcraft.io](https://snapcraft.io/).
2.  Reserve your application name on the dashboard (e.g., `quran-nur-desktop`).
3.  Install the Snapcraft CLI on your development machine:
    ```bash
    sudo snap install snapcraft --classic
    ```

### Building and Publishing
1.  Open your terminal and navigate to the project root.
2.  Run the build command:
    ```bash
    snapcraft
    ```
3.  Once the build finishes, it will generate a `.snap` file (e.g., `quran-nur-desktop_0.1.2_amd64.snap`).
4.  Test it locally:
    ```bash
    sudo snap install ./quran-nur-desktop_0.1.2_amd64.snap --dangerous
    ```
5.  If it works perfectly, upload it to the store:
    ```bash
    snapcraft upload quran-nur-desktop_0.1.2_amd64.snap --release=stable
    ```

---

## 2. Publishing to Flathub (Flatpak)

The base configuration file is located at `flatpak/com.quran.desktop.yml`.

Flathub is very strict. Their build servers disable internet access entirely during the build for security and reproducibility. This means we must provide explicit lists of every single dependency.

### Prerequisites
1.  Install the `flatpak` and `flatpak-builder` tools on your Linux machine.
2.  Install the Flathub dependency generators:
    *   [flatpak-node-generator](https://github.com/flatpak/flatpak-node-generator)
    *   [flatpak-cargo-generator](https://github.com/flatpak/flatpak-cargo-generator)

### Building and Publishing
1.  Generate the offline Node.js dependencies:
    ```bash
    flatpak-node-generator npm package-lock.json -o flatpak/node-sources.json
    ```
2.  Generate the offline Rust dependencies:
    ```bash
    python3 flatpak-cargo-generator.py src-tauri/Cargo.lock -o flatpak/cargo-sources.json
    ```
3.  Uncomment the `cargo-sources.json` and `node-sources.json` lines at the bottom of the `flatpak/com.quran.desktop.yml` file.
4.  Test the build locally:
    ```bash
    flatpak-builder build-dir flatpak/com.quran.desktop.yml --force-clean
    ```
5.  If the build succeeds, you are ready to submit it to the store!
6.  Fork the [Flathub repository](https://github.com/flathub/flathub) on GitHub.
7.  Add your `com.quran.desktop.yml`, `node-sources.json`, and `cargo-sources.json` files to a new branch in your fork.
8.  Open a Pull Request on the Flathub repository to have your app reviewed and published.
