import org.gradle.api.tasks.Exec

val remoteHost = "dsilbergleithcu@worklaptop.local"        // your Mac's SSH user/host
val remoteDir = "~/AndroidStudioProjects/Deadly"              // path on the Mac where sources live
val gradleTask = "iosX64Binaries"        // or iosX64Run, iosArm64Binaries, etc.

tasks.register<Exec>("iosRemoteRunSimulator") {
    group = "build"
    description = "Sync sources to Mac and run iOS build remotely"

        //ssh $remoteHost "cd $remoteDir && ./gradlew $gradleTask"
    // Step 1: sync files
    commandLine("bash", "-c", """
        rsync -az --delete --exclude '.gradle' --exclude 'build' ./ $remoteHost:$remoteDir &&
        ssh $remoteHost "cd $remoteDir && make run-ios-simulator"
    """.trimIndent())
}

