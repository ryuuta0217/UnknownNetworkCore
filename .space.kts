/**
* JetBrains Space Automation
* This Kotlin script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build") {
  env["CI"] = true
  gradlew("openjdk:17", "clean downloadWaterfall reCompressWaterfall compileJava jar shadowJar reobfJar")
}
