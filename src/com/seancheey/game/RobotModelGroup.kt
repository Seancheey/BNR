package com.seancheey.game

import java.io.Serializable

/**
 * Created by Seancheey on 27/05/2017.
 * GitHub: https://github.com/Seancheey
 */
data class RobotModelGroup(var robotModels: List<RobotModel>) : Serializable, Iterable<RobotModel> {
    class RobotModelIterator(val robotModels: List<RobotModel>) : Iterator<RobotModel> {
        var cursor = 0
        override fun hasNext(): Boolean {
            return cursor < robotModels.size
        }

        override fun next(): RobotModel {
            if (hasNext()) {
                val current = robotModels[cursor]
                cursor++
                return current
            }
            throw NoSuchElementException()
        }

    }

    override fun iterator(): Iterator<RobotModel> {
        return RobotModelIterator(robotModels)
    }

    init {
        if (robotModels.size > Config.botGroupNum) {
            robotModels = robotModels.slice(0 until Config.botGroupNum)
        }
    }
}