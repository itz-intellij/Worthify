plugins {
    java
    id("com.gradleup.shadow") version "8.3.9"
}

group = (project.findProperty("group") as String?) ?: "dev.simpleye"
version = (project.findProperty("version") as String?) ?: "1.0.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")

    implementation("org.bstats:bstats-bukkit:3.0.2")

    implementation("org.xerial:sqlite-jdbc:3.46.1.0")

    // Optional hooks
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("org.bstats", "dev.simpleye.worthify.lib.bstats")
    relocate("org.sqlite", "dev.simpleye.worthify.lib.sqlite")
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
