// Khoá ký bản phát hành KHÔNG nằm trong repo.
// Đặt 4 dòng sau trong %USERPROFILE%\.gradle\gradle.properties (hoặc biến môi trường cùng tên):
//   GLASSDECK_KEYSTORE=C:\\duong\\dan\\toi\\release.jks
//   GLASSDECK_KEYSTORE_PASSWORD=...
//   GLASSDECK_KEY_ALIAS=...
//   GLASSDECK_KEY_PASSWORD=...
// Không có các giá trị này thì bản release được build nhưng chưa ký.
extra["releaseSigning"] = run {
    fun value(name: String): String? =
        (findProperty(name) as String?) ?: System.getenv(name)
    val store = value("GLASSDECK_KEYSTORE")
    if (store == null || !file(store).exists()) null
    else mapOf(
        "storeFile" to store,
        "storePassword" to value("GLASSDECK_KEYSTORE_PASSWORD"),
        "keyAlias" to value("GLASSDECK_KEY_ALIAS"),
        "keyPassword" to value("GLASSDECK_KEY_PASSWORD"),
    )
}
