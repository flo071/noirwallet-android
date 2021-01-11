package org.noirofficial.tools.util

import android.content.Context
import java.util.*

object SeedUtil {

    fun getWordList(ctx: Context): List<String> {
        var languageCode: String? = Locale.getDefault().language
        if (languageCode == null) {
            languageCode = "en"
        }
        return Bip39Reader.bip39List(ctx, languageCode)
    }
}