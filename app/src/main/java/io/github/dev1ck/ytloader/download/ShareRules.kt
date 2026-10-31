package io.github.dev1ck.ytloader.download

/** 저장한 파일의 공유·폴더 열기 규칙 (FR-12). */
object ShareRules {
    /** 외부 저장소 문서 제공자에서 `Download/YTLoader`의 문서 ID. */
    const val ROOT_DOCUMENT_ID = "primary:Download/YTLoader"

    /** [subDir]: 작업 폴더 기준 하위 경로(재생목록 폴더). 비었으면 루트. */
    fun folderDocumentId(subDir: String?): String {
        val sub = subDir.orEmpty().trim('/')
        return if (sub.isEmpty()) ROOT_DOCUMENT_ID else "$ROOT_DOCUMENT_ID/$sub"
    }

    /** 여러 파일을 함께 공유할 때의 MIME: 모두 같으면 그대로, 대분류만 같으면 `audio/*` 같은 형태, 아니면 `*/*`. */
    fun shareMimeType(mimeTypes: List<String>): String {
        val distinct = mimeTypes.distinct()
        if (distinct.size == 1) return distinct[0]
        val top = distinct.map { it.substringBefore('/') }.distinct().singleOrNull()
        return if (top.isNullOrEmpty() || top == "*") "*/*" else "$top/*"
    }

    /** 파일들이 한 하위 폴더에 모여 있으면 그 폴더, 섞여 있거나 루트면 null. */
    fun commonSubDir(subDirs: List<String>): String? = subDirs.distinct().singleOrNull()?.takeIf { it.isNotEmpty() }
}
