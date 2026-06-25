plugins {
    java
}

group = "com.example"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.34-alpha")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.4.0-SNAPSHOT")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}
