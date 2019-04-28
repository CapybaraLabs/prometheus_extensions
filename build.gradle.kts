plugins {
    `java-library`
    `maven-publish`
}

group = "space.npstr.prometheus_extensions"
version = "0.0.1-SNAPSHOT"

repositories {
    jcenter()
}

val prometheusVersion   = "0.6.0"
val dsProxyVersion      = "1.5.1"
val okHttpVersion       = "3.14.1"
val jdaVersion          = "3.8.3_463"
val troveVersion        = "3.0.3"
val fastutilVersion     = "8.2.2"

dependencies {
    api("io.prometheus:simpleclient:$prometheusVersion")

    compileOnly("net.ttddyy:datasource-proxy:$dsProxyVersion")
    compileOnly("com.squareup.okhttp3:okhttp:$okHttpVersion")

    compileOnly("net.dv8tion:JDA:$jdaVersion")
    // required for user count hax
    compileOnly("net.sf.trove4j:trove4j:$troveVersion")
    compileOnly("it.unimi.dsi:fastutil:$fastutilVersion")
}

publishing {
    publications {
        create<MavenPublication>("prometheusExtensions") {
            from(components["java"])
        }
    }
}
