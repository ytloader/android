package io.github.ytloader.download

import io.github.ytloader.download.ErrorHintMapper.LOGIN_HINT
import io.github.ytloader.download.ErrorHintMapper.UPDATE_HINT
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorHintMapperTest {
    private val withLogin = "$UPDATE_HINT\n$LOGIN_HINT"

    @Test fun eachKeyword() {
        assertEquals(withLogin, ErrorHintMapper.hint("ERROR: [youtube] x: Sign in to confirm you’re not a bot"))
        assertEquals(withLogin, ErrorHintMapper.hint("ERROR: login required"))
        assertEquals(withLogin, ErrorHintMapper.hint("ERROR: [youtube] x: Private video"))
        assertEquals(withLogin, ErrorHintMapper.hint("ERROR: Join this channel to get access to members-only content"))
        assertEquals(withLogin, ErrorHintMapper.hint("ERROR: Sign in to Confirm Your Age"))
    }

    @Test fun caseInsensitive() = assertEquals(withLogin, ErrorHintMapper.hint("ERROR: PRIVATE VIDEO"))

    @Test fun generalError() =
        assertEquals(UPDATE_HINT, ErrorHintMapper.hint("ERROR: [youtube] x: This video is unavailable"))

    @Test fun nullError() = assertEquals(UPDATE_HINT, ErrorHintMapper.hint(null))
}
