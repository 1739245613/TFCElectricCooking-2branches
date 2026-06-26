plugins {
    java
    idea
    id("net.minecraftforge.gradle") version "[6.0,6.2)"
    id("org.parchmentmc.librarian.forgegradle") version "1.+"
    id("org.spongepowered.mixin") version "0.7.+"
}

val minecraftVersion = "1.20.1"
val forgeVersion = "47.1.3"
val mixinVersion = "0.8.5"
val parchmentVersion = "2023.09.03-1.20.1"
val jeiVersion = "15.2.0.21"
val jadeVersion = "4614153"
val tfcCurseVersion = "5872631"

val modId = "tfcelectriccooking"
val modVersion = System.getenv("VERSION") ?: "2.0.1"
val tfcSourceDir = "../TerraFirmaCraft-3.2.21-1.20"

val tfcLocalJars = fileTree("$tfcSourceDir/build/libs") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}

group = "com.tfcelectriccooking"
version = modVersion

base {
    archivesName.set("TFCElectricCooking-Forge-$minecraftVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

idea {
    module {
        excludeDirs.add(file("run"))
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven(url = "https://dvs1.progwml6.com/files/maven/")
    maven(url = "https://modmaven.k-4u.nl")
    maven(url = "https://maven.blamejared.com")
    maven(url = "https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    minecraft("net.minecraftforge", "forge", "$minecraftVersion-$forgeVersion")

    if (tfcLocalJars.files.isNotEmpty()) {
        compileOnly(files(tfcLocalJars))
        runtimeOnly(files(tfcLocalJars))
    } else {
        compileOnly(fg.deobf("curse.maven:tfc-302973:$tfcCurseVersion"))
        runtimeOnly(fg.deobf("curse.maven:tfc-302973:$tfcCurseVersion"))
    }

    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-common-api:$jeiVersion"))
    compileOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge-api:$jeiVersion"))
    runtimeOnly(fg.deobf("mezz.jei:jei-$minecraftVersion-forge:$jeiVersion"))

    compileOnly(fg.deobf("curse.maven:jade-324717:$jadeVersion"))
    runtimeOnly(fg.deobf("curse.maven:jade-324717:$jadeVersion"))

    if (System.getProperty("idea.sync.active") != "true") {
        annotationProcessor("org.spongepowered:mixin:$mixinVersion:processor")
    }
}

minecraft {
    mappings("parchment", parchmentVersion)

    runs {
        all {
            property("forge.logging.console.level", "debug")
            args("-mixin.config=$modId.mixins.json")
            property("mixin.env.remapRefMap", "true")
            property("mixin.env.refMapRemappingFile", "$projectDir/build/createSrgToMcp/output.srg")
            jvmArgs("-ea", "-Xmx4G", "-Xms2G")

            mods.create(modId) {
                source(sourceSets.main.get())
            }
        }

        register("client") {
            workingDirectory(project.file("run/client"))
        }

        register("server") {
            workingDirectory(project.file("run/server"))
            arg("--nogui")
        }
    }
}

mixin {
    add(sourceSets.main.get(), "$modId.refmap.json")
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.processResources {
    inputs.property("jarVersion", project.version)
    filesMatching("META-INF/mods.toml") {
        expand("file" to mapOf("jarVersion" to project.version))
    }
    from(rootDir) {
        include("LICENSE", "DISCLAIMER.md")
        into("META-INF")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "TFC Electric Cooking",
            "Implementation-Version" to project.version,
            "MixinConfigs" to "$modId.mixins.json",
            "Bundle-License" to "All Rights Reserved",
            "Bundle-Disclaimer" to "See META-INF/DISCLAIMER.md"
        )
    }
}
