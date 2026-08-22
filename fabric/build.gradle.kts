plugins {
    id("net.fabricmc.fabric-loom")
}

loom {
    accessWidenerPath = file("src/main/resources/knockbacksync.accesswidener")
}

base {
    archivesName.set("${rootProject.property("archives_base_name")}-fabric")
}

tasks.named<JavaCompile>("compileJava") {
    source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<Javadoc>().configureEach {
    source(project(":common").sourceSets.main.get().allJava)
}

repositories {
    maven("https://jitpack.io") // Conditional Mixin
}

dependencies {
    implementation(project(":common"))

    minecraft("com.mojang:minecraft:${rootProject.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${rootProject.property("loader_version")}")
    implementation(fabricApi.module("fabric-lifecycle-events-v1", "${rootProject.property("fabric_version")}"))
    implementation(fabricApi.module("fabric-events-interaction-v0", "${rootProject.property("fabric_version")}"))

    include(implementation("me.lucko:fabric-permissions-api:0.7.0")!!)
    include(implementation("com.github.retrooper:packetevents-fabric:2.13.0")!!)
    include(implementation("org.incendo:cloud-fabric:2.0.0")!!)

    include(implementation("org.incendo:cloud-minecraft-extras:2.0.0")!!)
    include(implementation("org.yaml:snakeyaml:2.6")!!)
    include(implementation("org.kohsuke:github-api:1.330")!!)
    // Required for org.kohsuke.github
    include(implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")!!)
    include(implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")!!)
    include(implementation("com.fasterxml.jackson.core:jackson-core:2.20.0")!!)
    //    Not requires in modern Minecraft. May be needed if fabric version is backported to older versions
    //    include(implementation("org.apache.commons:commons-lang3:3.17.0")!!)
    //    include(implementation("commons-io:commons-io:2.16.1")!!)

    compileOnly("com.github.retrooper:packetevents-api:2.13.0")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    // Compile-time signature dependency only; it is not included in the mod.
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("ac.grim.grimac:GrimAPI:1.6.0.10") {
        isTransitive = false
    }
    compileOnly("org.projectlombok:lombok:1.18.46")
    compileOnly("io.netty:netty-all:4.1.72.Final")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)

    inputs.property("version", project.version)
    inputs.property("minecraft_version", rootProject.property("minecraft_version"))
    inputs.property("loader_version", rootProject.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to rootProject.property("minecraft_version")!!,
            "loader_version" to rootProject.property("loader_version")!!
        )
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}
