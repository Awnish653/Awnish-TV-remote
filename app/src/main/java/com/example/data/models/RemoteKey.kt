package com.example.data.models

enum class RemoteKey(val label: String, val keyCode: Int) {
    POWER("Power", 26), // KEYCODE_POWER
    DPAD_UP("Up", 19), // KEYCODE_DPAD_UP
    DPAD_DOWN("Down", 20), // KEYCODE_DPAD_DOWN
    DPAD_LEFT("Left", 21), // KEYCODE_DPAD_LEFT
    DPAD_RIGHT("Right", 22), // KEYCODE_DPAD_RIGHT
    SELECT("OK", 23), // KEYCODE_DPAD_CENTER
    BACK("Back", 4), // KEYCODE_BACK
    HOME("Home", 3), // KEYCODE_HOME
    MENU("Menu", 82), // KEYCODE_MENU
    VOLUME_UP("Vol +", 24), // KEYCODE_VOLUME_UP
    VOLUME_DOWN("Vol -", 25), // KEYCODE_VOLUME_DOWN
    VOLUME_MUTE("Mute", 164), // KEYCODE_VOLUME_MUTE
    CHANNEL_UP("Ch +", 166), // KEYCODE_CHANNEL_UP
    CHANNEL_DOWN("Ch -", 167), // KEYCODE_CHANNEL_DOWN
    PLAY_PAUSE("Play/Pause", 85), // KEYCODE_MEDIA_PLAY_PAUSE
    VOICE_SEARCH("Voice", 84), // KEYCODE_SEARCH
    DIGIT_0("0", 7), // KEYCODE_0
    DIGIT_1("1", 8), // KEYCODE_1
    DIGIT_2("2", 9), // KEYCODE_2
    DIGIT_3("3", 10), // KEYCODE_3
    DIGIT_4("4", 11), // KEYCODE_4
    DIGIT_5("5", 12), // KEYCODE_5
    DIGIT_6("6", 13), // KEYCODE_6
    DIGIT_7("7", 14), // KEYCODE_7
    DIGIT_8("8", 15), // KEYCODE_8
    DIGIT_9("9", 16), // KEYCODE_9
    KEYBOARD_INPUT("Text", 0) // Special action
}
