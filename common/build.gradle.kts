import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("com.github.gmazzo.buildconfig") version "6.0.10"
}

val shadePE: Boolean by rootProject.extra

dependencies {
    // True compileOnly deps
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    // GrimAPI 1.6 retains a legacy Bukkit Player overload. KBS only calls the
    // UUID overload, but javac still needs the signature type while compiling.
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.72.Final")
    compileOnly("org.projectlombok:lombok:1.18.46")
    compileOnly("ac.grim.grimac:GrimAPI:1.6.0.10") {
        isTransitive = false
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    // Shaded in or bundled by platform-specific code
    if (shadePE) {
        implementation("com.github.retrooper:packetevents-api:2.13.1+4d40422-SNAPSHOT")
    } else {
        compileOnly("com.github.retrooper:packetevents-api:2.13.1+4d40422-SNAPSHOT")
    }

    implementation("org.yaml:snakeyaml:2.6")
    implementation("org.kohsuke:github-api:1.330") {
        exclude(group = "commons-io", module = "commons-io")
        exclude(group = "org.apache.commons", module = "commons-lang3")
    }

    implementation("org.incendo:cloud-core:2.1.0")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

buildConfig {
    buildConfigField("String", "GITHUB_REPO", "\"${project.rootProject.ext["githubRepo"]}\"")
}
