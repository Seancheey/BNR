package com.seancheey.game.command

import com.seancheey.game.Action
import com.seancheey.game.model.MovableNode
import com.seancheey.game.Player

/**
 * Created by Seancheey on 04/06/2017.
 * GitHub: https://github.com/Seancheey
 */
class MoveCommand(from: Player, to: MovableNode, toX: Double, toY: Double) : Command(from, to, {
    to.move()
    to.actionTree.putAction(Action.gotoTargetAction(toX, toY), Action.CUSTOM_ACTION)
})