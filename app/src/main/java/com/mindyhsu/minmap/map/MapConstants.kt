package com.mindyhsu.minmap.map

/**
 * Navigation status
 * [NAVI_INIT] User has current event but not start navigation yet
 * [NAVI_ING] Doing navigation
 * [NAVI_PAUSE] MapFragment is not onResume ?
 * [ON_INVITE] User is creating event
 * [ON_RESET] There's no current event
 * */
enum class OperationStatus {
    NAVI_INIT,
    NAVI_ING,
    NAVI_PAUSE,
    ON_INVITE,
    ON_RESET
}

/** Navigation mode */
const val WALKING_MODE = "walking"

/** Navigation instruction */
const val STEP_END_LOCATION = "stepEndLocation"
const val FINAL_STEP_LOCATION = "finalStepLocation"
const val DIRECTION = "direction"
const val DIRECTION_GO_STRAIGHT = "Go Straight"
const val DIRECTION_TURN_RIGHT = "Turn Right"
const val DIRECTION_TURN_LEFT = "Turn Left"
const val DIRECTION_RIGHT = "right"
const val DIRECTION_LEFT = "left"
const val INSTRUCTION_SPLIT_ON = "on "
const val INSTRUCTION_SPLIT_ONTO = "onto "
const val INSTRUCTION_SPLIT_FINAL_DIRECTION = "Destination will be on the "
const val DISTANCE_DURATION = "distanceAndDuration"
const val METERS_FROM_THE_LAST_STEP = 20

/** Map zoom status */
const val DEFAULT_ZOOM = 15F
const val FOCUS_ZOOM = 17F

/** Polyline */
const val POLYLINE_WIDTH = 15F

/** Location manager */
const val LOCATION_MANAGER_TIME = 0L
const val LOCATION_MANAGER_DISTANCE = 0F

/** Text to speech */
const val SPEECH_RATE = 0.75F
const val REMINDER_DURATION = 20
const val TEXT_TO_SPEECH_MESSAGE = "Text to speech: distance="
