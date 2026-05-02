plugins {
    id("net.neoforged.moddev") version "2.0.107"
}

val minecraftVersion: String = "1.21.1"
val neoForgeVersion: String = "21.1.197"
val parchmentVersion: String = "2024.11.17"
val parchmentMinecraftVersion: String = "1.21.1"
val jeiVersion: String = "19.25.0.321"

val modId: String = "tfcelectriccooking"
val modVersion: String = System.getenv("VERSION") ?: "1.0.2"
val modJavaVersion: String = "21"

// Paths to reference source dependencies (compiled classes)
val tfcSourceDir: String = "C:/Users/g1739/Desktop/群峦前置源码/1.21/TerraFirmaCraft-4.1.0"
val firmaLifeSourceDir: String = "C:/Users/g1739/Desktop/群峦前置源码/1.21/firmalife-1.21.x"
val ieSourceDir: String = "C:/Users/g1739/Desktop/群峦前置源码/1.21/ImmersiveEngineering-12.4.2-194"

group = "com.tfcelectriccooking"
version = modVersion

neoForge {
    version = neoForgeVersion
}

base {
    archivesName.set("TFCElectricCooking-NeoForge-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
    mavenCentral()
    mavenLocal()
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com/") }
        filter { includeGroup("mezz.jei") }
    }
    exclusiveContent {
        forRepository { maven("https://www.cursemaven.com") }
        filter { includeGroup("curse.maven") }
    }
}

sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    from(rootDir) {
        include("LICENSE", "DISCLAIMER.md")
        into("META-INF")
    }
}

neoForge {
    validateAccessTransformers = true

    parchment {
        minecraftVersion.set(parchmentMinecraftVersion)
        mappingsVersion.set(parchmentVersion)
    }

    runs {
        configureEach {
            jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition", "-ea")
        }
        register("client") {
            client()
            gameDirectory = file("run/client")
        }
        register("server") {
            server()
            gameDirectory = file("run/server")
            programArgument("--nogui")
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly("mezz.jei:jei-${minecraftVersion}-common-api:${jeiVersion}")
    compileOnly("mezz.jei:jei-${minecraftVersion}-neoforge-api:${jeiVersion}")
    runtimeOnly("mezz.jei:jei-${minecraftVersion}-neoforge:${jeiVersion}")
    implementation(group = "curse.maven", name = "jade-324717", version = "6853386")

    // TFC - try compiled jar first, then fall back to classes directory
    val tfcJar = file("$tfcSourceDir/build/libs").listFiles()?.firstOrNull { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }
    val tfcClasses = file("$tfcSourceDir/build/classes/java/main")
    val tfcResources = file("$tfcSourceDir/build/resources/main")
    val tfcGenResources = file("$tfcSourceDir/src/generated/resources")

    if (tfcJar != null && tfcJar.exists()) {
        compileOnly(files(tfcJar))
    } else {
        if (tfcClasses.exists()) compileOnly(files(tfcClasses))
        if (tfcResources.exists()) compileOnly(files(tfcResources))
    }
    if (tfcGenResources.exists()) compileOnly(files(tfcGenResources))

    // FirmaLife - try compiled jar first, then fall back to classes directory
    val flJar = file("$firmaLifeSourceDir/build/libs").listFiles()?.firstOrNull { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }
    val flClasses = file("$firmaLifeSourceDir/build/classes/java/main")
    val flResources = file("$firmaLifeSourceDir/build/resources/main")
    val flGenResources = file("$firmaLifeSourceDir/src/generated/resources")

    if (flJar != null && flJar.exists()) {
        compileOnly(files(flJar))
    } else {
        if (flClasses.exists()) compileOnly(files(flClasses))
        if (flResources.exists()) compileOnly(files(flResources))
    }
    if (flGenResources.exists()) compileOnly(files(flGenResources))

    // Immersive Engineering - need both api and main classes
    val ieJar = file("$ieSourceDir/build/libs").listFiles()?.firstOrNull { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") && !it.name.contains("api") }
    val ieApiClasses = file("$ieSourceDir/build/classes/java/api")
    val ieMainClasses = file("$ieSourceDir/build/classes/java/main")
    val ieResources = file("$ieSourceDir/build/resources/main")
    val ieGenResources = file("$ieSourceDir/src/generated/resources")

    if (ieJar != null && ieJar.exists()) {
        compileOnly(files(ieJar))
    } else {
        if (ieApiClasses.exists()) compileOnly(files(ieApiClasses))
        if (ieMainClasses.exists()) compileOnly(files(ieMainClasses))
        if (ieResources.exists()) compileOnly(files(ieResources))
    }
    if (ieGenResources.exists()) compileOnly(files(ieGenResources))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val jarVersion = modVersion
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("file" to mapOf("jarVersion" to jarVersion))
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "TFC Electric Cooking",
            "Implementation-Version" to project.version,
            "Bundle-License" to "All Rights Reserved",
            "Bundle-Disclaimer" to "See META-INF/DISCLAIMER.md"
        )
    }
}
