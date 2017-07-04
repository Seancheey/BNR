package com.seancheey.game.battlefield

import com.seancheey.game.*
import com.seancheey.game.model.Node
import com.seancheey.game.model.RobotModel
import com.seancheey.game.model.RobotNode
import java.io.Serializable


/**
 * Created by Seancheey on 29/05/2017.
 * GitHub: https://github.com/Seancheey
 */
interface Battlefield : Serializable {
    var name: String
    val width: Double
    val height: Double
    val nodes: ArrayList<Node>
    val players: ArrayList<PlayerInGame>

    fun putRobot(model: RobotModel, x: Double, y: Double, speed: Double = 0.0, orientation: Double = 0.0) {
        val botNode = RobotNode(model, this, x, y)
        botNode.speed = speed
        botNode.orientation = orientation
        nodes.add(botNode)
    }
}