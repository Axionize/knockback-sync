import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.java.TargetJvmVersion

plugins {
    id("com.gradleup.shadow")
}

val shadePE: Boolean by rootProject.extra

base {
    archivesName.set("${rootProject.property("archives_base_name")}-bukkit${if (shadePE) "" else "-lite"}")
}

val shadeThisThing: Configuration by configurations.creating {
    isCanBeConsumed = true
    isTransitive = true
}

tasks.named<JavaCompile>("compileJava") {
    source(project(":common").sourceSets.main.get().allSource)
}

tasks.withType<Javadoc>().configureEach {
    source(project(":common").sourceSets.main.get().allJava)
}

dependencies {
    implementation(project(":common"))

    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.mojang:brigadier:1.3.10")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("io.netty:netty-all:4.1.72.Final")
    compileOnly("ac.grim.grimac:GrimAPI:1.6.0.10") {
        isTransitive = false
        // Grim requires Java 17; KBS itself continues to emit Java 8 bytecode.
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")

    shadeThisThing(implementation("org.kohsuke:github-api:1.330")!!)
    if (shadePE) {
        shadeThisThing(implementation("com.github.retrooper:packetevents-spigot:2.13.0")!!)
    } else {
        compileOnly("com.github.retrooper:packetevents-spigot:2.13.0")
    }
    shadeThisThing(implementation("org.incendo:cloud-paper:2.0.0")!!)
    shadeThisThing(implementation("org.incendo:cloud-core:2.1.0")!!)

    // Required for 1.14.4 support because gson is too old to have JsonParser.parseString().
    shadeThisThing(implementation("com.google.code.gson:gson:2.14.0")!!)

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testImplementation("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    testImplementation("ac.grim.grimac:GrimAPI:1.6.0.10") {
        isTransitive = false
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 17)
        }
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    // Remove the -all suffix from the output JAR file name
    archiveClassifier.set("")

    configurations = listOf(shadeThisThing)
    enableAutoRelocation = true
    relocationPrefix = "${project.property("maven_group")}.${project.property("archives_base_name")}.shaded"
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    from(project(":common").sourceSets.main.get().resources)
    inputs.property("version", project.version)
    inputs.property("shadePE", shadePE) // Add shadePE as an input to trigger reprocessing if it changes
    filteringCharset = "UTF-8"


    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "depends" to if (shadePE) "[]" else listOf("packetevents")
        )
    }
}
