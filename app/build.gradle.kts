plugins { alias(libs.plugins.android); alias(libs.plugins.kotlin); alias(libs.plugins.compose); alias(libs.plugins.ksp) }
android { namespace="com.imux.player"; compileSdk=36
 defaultConfig { applicationId="com.imux.player"; minSdk=26; targetSdk=36; versionCode=1; versionName="0.1.0" }
 compileOptions { sourceCompatibility=JavaVersion.VERSION_17; targetCompatibility=JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget="17" }; buildFeatures { compose=true }
}
dependencies {
 implementation(platform(libs.bom)); implementation(libs.core); implementation(libs.activity); implementation(libs.lifecycle); implementation(libs.viewmodel); implementation(libs.navigation); implementation(libs.ui); implementation(libs.icons); implementation(libs.material3)
 implementation(libs.room); implementation(libs.roomktx); ksp(libs.room)
 implementation(libs.datastore); implementation(libs.documentfile); implementation(libs.exo); implementation(libs.session)
}
