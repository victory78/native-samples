package org.gradle.samples.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
class MergePropertyList extends DefaultTask {
    @InputFiles
    final ConfigurableFileCollection sources = project.files()

    @Input
    final Property<String> module = project.objects.property(String)

    @Input
    final Property<String> identifier = project.objects.property(String)

    @OutputFile
    final RegularFileProperty outputFile = newOutputFile()

    @TaskAction
    private void doMerge() {
        File xmlPlist = project.file("${temporaryDir}/Info.plist")
        xmlPlist.delete()

        // Note: There seems to be a limit of how many command can be passed to the tool so we are using several invocation

        // Merge plist files
        project.exec {
            executable plistBuddyExecutable.absolutePath
            for (File source : sources) {
                args "-c", "Merge ${source.getAbsolutePath()}"
            }
            args "-c", "Save"
            args xmlPlist.absolutePath
            standardOutput = new FileOutputStream(project.file("$temporaryDir/outputs.txt"))
        }.assertNormalExitValue()

        // Add information automatically added by Xcode, part 1
        project.exec {
            executable plistBuddyExecutable.absolutePath
            args "-c", "Add :DTSDKName string iphonesimulator11.2"
            args "-c", "Add :DTXcode string 0920"
            args "-c", "Add :DTSDKBuild string 15C107"
            args "-c", "Add :BuildMachineOSBuild string 17D102"
            args "-c", "Add :DTPlatformName string iphonesimulator"
            args "-c", "Add :CFBundleSupportedPlatforms array"
            args "-c", "Add :CFBundleSupportedPlatforms:0 string iPhoneSimulator"
            args "-c", "Add :DTCompiler string com.apple.compilers.llvm.clang.1_0"
            args "-c", "Save"
            args xmlPlist.getAbsolutePath()
            standardOutput = new FileOutputStream(project.file("$temporaryDir/outputs.txt"), true)
        }.assertNormalExitValue()

        // Add information automatically added by Xcode, part 2
        project.exec {
            executable plistBuddyExecutable.absolutePath
            args "-c", "Add :MinimumOSVersion string 11.2"
            args "-c", "Add :DTPlatformVersion string 11.2"
            args "-c", "Add :UIDeviceFamily array"
            args "-c", "Add :UIDeviceFamily:0 integer 1"
            args "-c", "Add :UIDeviceFamily:1 integer 2"
            args "-c", "Add :DTXcodeBuild string 9C40b"
            args "-c", "Add :DTPlatformBuild string"
            args "-c", "Save"
            args xmlPlist.getAbsolutePath()
            standardOutput = new FileOutputStream(project.file("$temporaryDir/outputs.txt"), true)
        }.assertNormalExitValue()

        xmlPlist.text = xmlPlist.text.replace('$(PRODUCT_NAME)', module.get()).replace('$(EXECUTABLE_NAME)', module.get()).replace('$(PRODUCT_BUNDLE_IDENTIFIER)', identifier.get()).replace('$(DEVELOPMENT_LANGUAGE)', "en")

        project.exec {
            executable plutilExecutable.absolutePath
            args "-convert", "binary1", "-o", outputFile.get().asFile.absolutePath, xmlPlist.absolutePath
            standardOutput = new FileOutputStream(project.file("$temporaryDir/outputs.txt"), true)
        }.assertNormalExitValue()
    }

    @InputFile
    protected File getPlistBuddyExecutable() {
        return new File("/usr/libexec/PlistBuddy")
    }

    @InputFile
    protected File getPlutilExecutable() {
        return new File("xcrun --sdk iphonesimulator --find plutil".execute().text.trim())
    }
}
