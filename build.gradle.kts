plugins {
    `java-library`
    `maven-publish`
    idea
    id("com.github.ben-manes.versions") version "0.44.0"
    id("org.ajoberstar.grgit") version "5.0.0"
}

group = "space.npstr.prometheus_extensions"
version = versionTag()
println("Version: ${project.version}")

java {
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        content { includeModule("net.dv8tion", "JDA") }
        content { includeGroup("club.minnced") }
    }
//    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") } // D4J snapshots
//    maven { url = uri("https://repo.spring.io/milestone") }                        // D4J snapshots
}

val prometheusVersion = "0.16.0"
val dsProxyVersion = "1.8.1"
val jdaVersion = "4.4.0_352"
val troveVersion = "3.0.3"
val fastutilVersion = "8.5.11"
// see https://oss.sonatype.org/content/repositories/snapshots/com/discord4j/discord4j-core/
val d4jCoreVersion = "3.2.3"
val jUnitVersion = "5.9.2"
val mockitoVersion = "5.0.0"
val assertJVersion = "3.24.2"

dependencies {
    api("io.prometheus:simpleclient:$prometheusVersion")

    compileOnly("net.ttddyy:datasource-proxy:$dsProxyVersion")

    compileOnly("net.dv8tion:JDA:$jdaVersion")
    // required for user count hax
    compileOnly("net.sf.trove4j:trove4j:$troveVersion")
    compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")

    compileOnly("com.discord4j:discord4j-core:$d4jCoreVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
    testImplementation("net.dv8tion:JDA:$jdaVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v\\-_]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    val isUnstable = listOf("ALPHA", "BETA").any { version.toUpperCase().contains(it) }
    return isUnstable || isStable.not()
}

// https://github.com/ben-manes/gradle-versions-plugin
tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

publishing {
    publications {
        create<MavenPublication>("prometheusExtensions") {
            from(components["java"])
        }
    }
}

fun versionTag(): String {
    val regex = Regex("^([0-9]+.[0-9]+.[0-9]+).*$")
    val match = regex.find(grgit.describe(mapOf("tags" to true)))

    return match?.value ?: "Unknown"
}
