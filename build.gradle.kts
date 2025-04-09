dependencies {
    // To change the versions, see the gradle.properties file
    minecraft("com.mojang:minecraft:${properties["minecraft_version"]}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"]}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"]}")

    // Fabric API
    modImplementation("net.fabricmc.fabric-api:fabric-api:${properties["fabric_version"]}")

    // Kotlin support
    modImplementation("net.fabricmc:fabric-language-kotlin:${properties["fabric_kotlin_version"]}")

    // Add SnakeYAML for YAML file parsing
    implementation("org.yaml:snakeyaml:2.0")

    // LuckPerms API
    modCompileOnly("net.luckperms:api:${properties["luckperms_version"]}")
} 