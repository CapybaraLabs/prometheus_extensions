plugins {
    `java-library`
    `maven-publish`
    `idea`
}

group = "space.npstr.prometheus_extensions"
version = "0.0.1-SNAPSHOT"

repositories {
    jcenter()
}

val prometheusVersion   = "0.8.0"
val dsProxyVersion      = "1.5.1"
val okHttpVersion       = "3.14.4"
val jdaVersion          = "4.0.0_54"
val troveVersion        = "3.0.3"
val fastutilVersion     = "8.3.0"
val jUnitVersion        = "5.5.2"
val mockitoVersion      = "3.1.0"
val assertJVersion      = "3.13.2"

dependencies {
    api("io.prometheus:simpleclient:$prometheusVersion")

    compileOnly("net.ttddyy:datasource-proxy:$dsProxyVersion")
    compileOnly("com.squareup.okhttp3:okhttp:$okHttpVersion")

    compileOnly("net.dv8tion:JDA:$jdaVersion")
    // required for user count hax
    compileOnly("net.sf.trove4j:trove4j:$troveVersion")
    compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")

    testRuntime("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
    testImplementation("net.dv8tion:JDA:$jdaVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("prometheusExtensions") {
            from(components["java"])
        }
    }
}
