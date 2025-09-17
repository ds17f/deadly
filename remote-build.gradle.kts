import org.gradle.api.tasks.Exec

val remoteHost = "dsilbergleithcu@worklaptop.local"        // your Mac's SSH user/host
val remoteDir = "~/AndroidStudioProjects/Deadly"              // path on the Mac where sources live
val gradleTask = "iosX64Binaries"        // or iosX64Run, iosArm64Binaries, etc.

tasks.register<Exec>("iosRemoteRunSimulator") {
    group = "build"
    description = "Sync sources to Mac and run iOS build remotely"

        //ssh $remoteHost "cd $remoteDir && ./gradlew $gradleTask"
    // Sync files and run iOS simulator remotely with zsh login shell
    commandLine("bash", "-c", """
        rsync -az --delete --exclude '.gradle' --exclude 'build' ./ $remoteHost:$remoteDir &&
        ssh $remoteHost "zsh -l -c 'cd $remoteDir && make run-ios-simulator'"
    """.trimIndent())
}

tasks.register<Exec>("androidRemoteRunEmulator") {
    group = "build"
    description = "Sync sources to remote machine and run Android emulator"

    // Sync files and run Android emulator remotely with zsh login shell and ANDROID_HOME
    commandLine("bash", "-c", """
        rsync -az --delete --exclude '.gradle' --exclude 'build' ./ $remoteHost:$remoteDir &&
        ssh $remoteHost "zsh -l -c 'export ANDROID_HOME=~/Library/Android/sdk && cd $remoteDir && make run-android-emulator'"
    """.trimIndent())
}

