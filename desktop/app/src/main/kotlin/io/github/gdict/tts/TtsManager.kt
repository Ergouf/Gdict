package io.github.gdict.tts

object TtsManager {

    private var audioPlayer: ((ByteArray) -> Unit)? = null

    fun setAudioPlayer(player: (ByteArray) -> Unit) {
        audioPlayer = player
    }

    fun speak(word: String, getDictAudio: ((String) -> ByteArray?)? = null): Boolean {
        try {
            val dictAudio = getDictAudio?.invoke(word)
            if (dictAudio != null) {
                audioPlayer?.invoke(dictAudio)
                return true
            }
        } catch (_: Exception) {
        }

        try {
            val edgeAudio = EdgeTtsClient.synthesize(word)
            if (edgeAudio != null && edgeAudio.isNotEmpty()) {
                audioPlayer?.invoke(edgeAudio)
                return true
            }
        } catch (_: Exception) {
        }

        return false
    }
}
