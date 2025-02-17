# Convert to toml vers plugin

The **Convert to TOML Plugin** for IntelliJ IDEA allows you to quickly convert dependencies from your Gradle project files into TOML format. This plugin is designed to make it easier to manage your project dependencies by transforming them into a standardized format compatible with various tools and systems that use TOML.

## Installation

To install the plugin, follow these steps:
1. Download the `.zip` plugin file from the [releases page](https://github.com/Balalaika73/TomlConvertPlugin/releases).
2. In Android Studio, go to **File → Settings → Plugins**.
3. Click on **Install Plugin from Disk...**.
4. Select the downloaded `.zip` file.
5. Restart Android Studio to activate the plugin.

## Usage

Once the plugin is installed:
1. Go to **Tools** in the top menu.
2. Click the **Convert to TOML** item.
3. The plugin will convert all dependencies in your `build.gradle.kts` and any other unconverted lines into TOML format.
4. Check the changes in the `build.gradle.kts` and `libs.version.toml` files.
5. Click **Sync Now** to apply the changes.

## Usage Example
Before convert:
![Before Convert](img/BeforeConvert.png)
After convert:
![After Convert](img/ConvertRes.png)
![After Convert Toml](img/ConvertResToml.png)

## Compatibility

- IntelliJ IDEA version: 2024.1 and above (maybe)
- Android Studio version: 2024.1 and above (maybe)
