plugins {
    `java-library`
    `maven-publish`
    idea
    id("com.github.ben-manes.versions") version "0.51.0"
    id("org.ajoberstar.grgit") version "5.3.0"
}

group = "space.npstr.prometheus_extensions"
version = versionTag()
println("Version: ${project.version}")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    withSourcesJar()
    consistentResolution {
        useCompileClasspathVersions()
    }
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

val prometheusVersion = "1.3.2"
val dsProxyVersion = "1.10"
val jdaVersion = "5.1.2"
val troveVersion = "3.0.3"
val fastutilVersion = "8.5.15"
// see https://oss.sonatype.org/content/repositories/snapshots/com/discord4j/discord4j-core/
val d4jCoreVersion = "3.2.6"
val jUnitPlatformVersion = "1.11.3"
val jUnitVersion = "5.11.3"
val mockitoVersion = "5.14.2"
val assertJVersion = "3.26.3"

dependencies {
    api("io.prometheus:prometheus-metrics-core:$prometheusVersion")

    compileOnly("net.ttddyy:datasource-proxy:$dsProxyVersion")

    compileOnly("net.dv8tion:JDA:$jdaVersion")
    // required for user count hax
    compileOnly("net.sf.trove4j:trove4j:$troveVersion")
    compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")

    compileOnly("com.discord4j:discord4j-core:$d4jCoreVersion")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:$jUnitPlatformVersion")
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
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v\\-_]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    val isUnstable = listOf("ALPHA", "BETA").any { version.uppercase().contains(it) }
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
