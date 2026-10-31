package io.github.dev1ck.ytloader.update

/** GitHub 릴리스 태그와 앱 버전 비교 (FR-15). `v0.2.0` 같은 숫자 태그만 다룬다. */
object ReleaseRules {
    /** `v0.2.0` / `0.2.0` → [0, 2, 0]. 숫자가 아닌 부분(`-beta` 등)이 있으면 null. */
    fun parse(version: String): List<Int>? {
        val parts = version.trim().removePrefix("v").removePrefix("V").split('.')
        return parts.map { it.toIntOrNull() ?: return null }
    }

    /** [tag]가 [current]보다 새 버전이면 true. 둘 중 하나라도 해석할 수 없으면 false. */
    fun isNewer(tag: String, current: String): Boolean {
        val a = parse(tag) ?: return false
        val b = parse(current) ?: return false
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** 릴리스 첨부 파일 중 설치할 APK 이름. */
    fun apkAsset(names: List<String>): String? = names.firstOrNull { it.endsWith(".apk", ignoreCase = true) }
}
