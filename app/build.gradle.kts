plugins { alias(libs.plugins.android); alias(libs.plugins.compose); alias(libs.plugins.ksp) }
android { namespace="com.imux.player"; compileSdk=37
 defaultConfig { applicationId="com.imux.player"; minSdk=26; targetSdk=37; versionCode=1; versionName="0.1.0" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 buildFeatures { compose=true }
 kotlin { compilerOptions {
   jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
   freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
   freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
 } }
}
dependencies {
 implementation(platform(libs.bom)); implementation(libs.core); implementation(libs.activity); implementation(libs.lifecycle); implementation(libs.viewmodel); implementation(libs.navigation); implementation(libs.ui); implementation(libs.icons); implementation(libs.material3)
 implementation(libs.room); implementation(libs.roomktx); ksp(libs.roomcompiler)
 implementation(libs.datastore); implementation(libs.documentfile); implementation(libs.exo); implementation(libs.session)
 testImplementation("junit:junit:4.13.2")
}
