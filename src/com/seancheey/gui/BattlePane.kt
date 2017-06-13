package com.seancheey.gui

import com.seancheey.game.*
import com.seancheey.game.battlefield.Battlefield
import com.seancheey.game.command.MoveCommand
import javafx.animation.AnimationTimer
import javafx.concurrent.Task
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.paint.Color
import javafx.scene.transform.Transform

/**
 * Created by Seancheey on 01/06/2017.
 * GitHub: https://github.com/Seancheey
 */
class BattlePane(val battlefield: Battlefield, width: Double, height: Double) : Canvas(width, height), GameInspector {
    override fun selectAllRobotsWithSameType(robotModel: RobotModel) {
        focusedNodes.clear()
        for (node in battlefield.nodes) {
            if (node is RobotNode) {
                if (node.model == robotModel) {
                    focusedNodes.add(node)
                }
            }
        }
    }

    override fun selectRobotBeside(x: Double, y: Double) {
        focusedNodes.clear()
        val minDistanceNode = nodesContainPoint(x, y).minBy { it.distanceTo(x, y) }
        if (minDistanceNode != null) {
            focusedNodes.add(minDistanceNode)
        }else{
        }
    }

    override fun moveFocusedRobotsTo(x: Double, y: Double) {
        focusedNodes.forEach { node -> if (node is MovableNode) gameDirector.command(MoveCommand(Config.player, node, x, y)) }
    }

    override var focusedNodes: ArrayList<Node> = arrayListOf()
    override var transX: Double = 0.0
    override var transY: Double = 0.0
    override var scale: Double = 1.0
        set(value) {
            scaleX = value
            scaleY = value
            scaleZ = value
            field = value
        }
    override val guiWidth: Double
        get() = width
    override val guiHeight: Double
        get() = height
    override val gameDirector: GameDirector = GameDirector(battlefield.nodes)
    private val renderTimer: AnimationTimer = object : AnimationTimer() {
        override fun handle(now: Long) {
            gameDirector.render(0.0)
        }
    }

    init {
        // set the background of battle field to light gray
        graphicsContext2D.fill = Color.LIGHTGRAY

        // render method
        gameDirector.render = {
            graphicsContext2D.fillRect(0.0, 0.0, this.width, this.height)
            graphicsContext2D.save()
            graphicsContext2D.scale(scale, scale)
            for (node in battlefield.nodes) drawNode(node)
            graphicsContext2D.restore()
        }

        setOnMouseClicked { event ->
            if (event.button == MouseButton.PRIMARY) {
                selectRobotBeside(event.x, event.y)
            }
            if (event.button == MouseButton.SECONDARY) {
                moveFocusedRobotsTo(event.x, event.y)
            }
            if (event.button == MouseButton.MIDDLE) {
                val nodes = nodesContainPoint(event.x, event.y).filter { it is RobotModel }
                if (nodes.isNotEmpty()) {
                    val firstNode: RobotModel = nodes[0] as RobotModel
                    selectAllRobotsWithSameType(firstNode)
                }
            }
        }

        setOnScroll { event ->
            if (event.deltaY > 0) {
                scale *= 1 - Config.scrollSpeedDelta
            } else if (event.deltaY < 0) {
                scale *= 1 + Config.scrollSpeedDelta
            }
        }

        start()
    }

    private fun nodesContainPoint(x: Double, y: Double): List<Node> {
        return gameDirector.nodes.filter { it.containsPoint(x, y) }
    }

    fun start() {
        if (!gameDirector.started) {
            Thread(object : Task<Unit>() {
                override fun call() {
                    gameDirector.start()
                }
            }).start()
            renderTimer.start()
        }
    }

    fun stop() {
        gameDirector.stop = true
        renderTimer.stop()
    }

    private fun drawNode(node: Node) {
        val rotate: Transform = Transform.rotate(node.degreeOrientation + 90.0, node.x, node.y)
        graphicsContext2D.setTransform(rotate.mxx, rotate.myx, rotate.mxy, rotate.myy, rotate.tx, rotate.ty)
        graphicsContext2D.drawImage(node.image, node.leftX, node.upperY, node.width, node.height)
        if (focusedNodes.contains(node)) {
            graphicsContext2D.strokeOval(node.leftX, node.upperY, node.width, node.height)
        }
    }
}