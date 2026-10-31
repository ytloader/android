package io.github.ytloader.download

/** 저장한 파일의 공유·폴더 열기 규칙 (FR-12). */
object ShareRules {
    /** 외부 저장소 문서 제공자에서 `Download/YTLoader`의 문서 ID. */
    const val ROOT_DOCUMENT_ID = "primary:Download/YTLoader"

    const val DEFAULT_LOCATION = "Download/YTLoader"

    /**
     * [subDir]: 작업 폴더 기준 하위 경로(재생목록 폴더). 비었으면 루트.
     * [rootDocumentId]: 사용자가 고른 폴더(SAF 트리)의 문서 ID. 외부 저장소 제공자는 `<볼륨>:<경로>` 형식이다.
     */
    fun folderDocumentId(subDir: String?, rootDocumentId: String = ROOT_DOCUMENT_ID): String {
        val sub = subDir.orEmpty().trim('/')
        val root = rootDocumentId.trimEnd('/')
        return when {
            sub.isEmpty() -> rootDocumentId
            root.endsWith(":") -> "$root$sub"
            else -> "$root/$sub"
        }
    }

    /**
     * 화면에 보일 저장 위치. [treeDocumentId]가 null이면 기본 위치.
     * `primary:Music/YT` → `Music/YT`, `1A2B-3C4D:Music` → `1A2B-3C4D/Music`(SD카드).
     */
    fun locationLabel(treeDocumentId: String?, subDir: String? = null): String {
        val base = when {
            treeDocumentId == null -> DEFAULT_LOCATION
            ':' !in treeDocumentId -> treeDocumentId
            else -> {
                val volume = treeDocumentId.substringBefore(':')
                val path = treeDocumentId.substringAfter(':').trim('/')
                when {
                    volume == "primary" -> path.ifEmpty { "내부 저장소" }
                    path.isEmpty() -> volume
                    else -> "$volume/$path"
                }
            }
        }
        val sub = subDir.orEmpty().trim('/')
        return if (sub.isEmpty()) base else "$base/$sub"
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
