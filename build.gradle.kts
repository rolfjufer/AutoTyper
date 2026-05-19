plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "ch.autotyper"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3")
        instrumentationTools()
        pluginVerifier()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "262.*"
        }
    }
    buildSearchableOptions = false
}
