import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("rainbow.base-conventions")
    id("rainbow.publish-conventions")
}

dependencies {
    // Implement namedElements so IDEs can use it correctly, but include the remapped build
    implementation(project(path = ":rainbow", configuration = "namedElements"))
    include(project(":rainbow"))

    implementation("io.github.spair:imgui-java-binding:1.90.0")
    implementation("io.github.spair:imgui-java-lwjgl3:1.90.0") {
        exclude(group = "org.lwjgl")
        exclude(group = "org.lwjgl.lwjgl")
    }
    implementation("io.github.spair:imgui-java-natives-windows:1.90.0")
    implementation("io.github.spair:imgui-java-natives-linux:1.90.0")
    implementation("io.github.spair:imgui-java-natives-macos:1.90.0")

    include("io.github.spair:imgui-java-binding:1.90.0")
    include("io.github.spair:imgui-java-lwjgl3:1.90.0")
    include("io.github.spair:imgui-java-natives-windows:1.90.0")
    include("io.github.spair:imgui-java-natives-linux:1.90.0")
    include("io.github.spair:imgui-java-natives-macos:1.90.0")
}

tasks {
    val copyJarTask = register<Copy>("copyRainbowClientJar") {
        group = "build"

        val remapJarTask = getByName<RemapJarTask>("remapJar")
        dependsOn(remapJarTask)

        from(remapJarTask.archiveFile)
        rename {
            "Rainbow.jar"
        }
        into(project.layout.buildDirectory.file("libs"))
    }

    named("build") {
        dependsOn(copyJarTask)
    }
}
