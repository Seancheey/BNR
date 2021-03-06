package com.seancheey.gui

import com.seancheey.game.Config
import com.seancheey.game.Game
import com.seancheey.game.GameDirector
import com.seancheey.game.model.GuiNode
import com.seancheey.game.model.Node
import com.seancheey.game.model.RobotModel
import com.seancheey.game.model.RobotNode
import javafx.scene.canvas.Canvas
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.scene.transform.Affine

/**
 * Created by Seancheey on 01/06/2017.
 * GitHub: https://github.com/Seancheey
 */
class BattleInspectPane(val battleCanvas: BattleCanvas) : AnchorPane(), GameInspector by battleCanvas {
    constructor(game: Game, clipWidth: Double, clipHeight: Double) : this(BattleCanvas(game, clipWidth, clipHeight))

    init {
        AnchorPane.setLeftAnchor(battleCanvas, 0.0)
        AnchorPane.setTopAnchor(battleCanvas, 0.0)
        children.add(battleCanvas)
        clip = Rectangle((battleCanvas.guiWidth - battleCanvas.clipWidth) / 2, (battleCanvas.guiHeight - battleCanvas.clipHeight) / 2, battleCanvas.clipWidth, battleCanvas.clipHeight)
        battleCanvas.drawGuiNode = { a, b -> drawGuiNode(a, b) }
        battleCanvas.clearGuiNode = { clearGuiNodes() }
    }

    fun drawGuiNode(node: GuiNode, parentNode: Node) {
        val parentX = parentNode.x * cameraScale - (battleCanvas.guiWidth * cameraScale - width) / 2 + cameraTransX
        val parentY = parentNode.y * cameraScale - (battleCanvas.guiHeight * cameraScale - height) / 2 + cameraTransY
        if (node.gui !in children) {
            children.add(node.gui)
        }
        AnchorPane.setLeftAnchor(node.gui, node.leftX + parentX)
        AnchorPane.setTopAnchor(node.gui, node.upperY + parentY)
    }

    fun clearGuiNodes() {
        val guiNodes = arrayListOf<GuiNode>()
        gameDirector.nodes.forEach {
            guiNodes.addAll(it.children.filterIsInstance<GuiNode>())
        }
        val guis = guiNodes.map { it.gui }
        val toRemove = children.filter { it !in guis && it != battleCanvas }

        children.removeAll(toRemove)
    }

    class BattleCanvas(override val game: Game, val clipWidth: Double, val clipHeight: Double) : Canvas(game.field.width, game.field.height), GameInspector {

        override fun clickRobot(model: RobotModel) {
            if (!model.empty) {
                battlefield.nodeAddQueue.add(RobotNode(model, battlefield, battlefield.width / 2, battlefield.height / 2, game.playerMap[Config.player]!!))
            }
        }

        override var cameraTransX: Double = 0.0
            set(value) {
                field = value
                translateX = value
                clipCanvas()
            }
        override var cameraTransY: Double = 0.0
            set(value) {
                field = value
                translateY = value
                clipCanvas()
            }
        override var cameraScale: Double = 1.0
            set(value) {
                scaleX = value
                scaleY = value
                scaleZ = value
                field = value
                clipCanvas()
            }
        override val guiWidth: Double
            get() = width
        override val guiHeight: Double
            get() = height
        override val gameDirector: GameDirector = GameDirector(game, { nodes, lag ->
            graphicsContext2D.restore()
            graphicsContext2D.fill = Color.LIGHTGRAY
            graphicsContext2D.fillRect(0.0, 0.0, this.width, this.height)
            graphicsContext2D.save()
            graphicsContext2D.fill = Color.ALICEBLUE
            graphicsContext2D.scale(cameraScale, cameraScale)

            nodes.forEach {
                drawNode(it)
            }

            // request focus to handle key event
            requestFocus()
            cameraTransX += vx
            cameraTransY += vy
            // clear Gui Nodes
            clearGuiNode()

        })
        private var vx: Double = 0.0
        private var vy: Double = 0.0
        var drawGuiNode: (GuiNode, Node) -> Unit = { _, _ -> }
        var clearGuiNode: () -> Unit = {}

        init {
            clipCanvas()
            fullMapScale()

            setOnMouseClicked { event ->
                if (event.button == MouseButton.PRIMARY) {
                    selectRobotBeside(event.x, event.y)
                }
                if (event.button == MouseButton.SECONDARY) {
                    moveFocusedRobotsTo(event.x, event.y)
                }
                if (event.button == MouseButton.MIDDLE) {
                    val nodes = gameDirector.nodes.filter { it.containsPoint(event.x, event.y) }.filterIsInstance<RobotModel>()
                    if (nodes.isNotEmpty()) {
                        val firstNode: RobotModel = nodes[0]
                        selectAllRobotsWithSameType(firstNode)
                    }
                }
            }

            setOnScroll { event ->
                if (event.deltaY > 0) {
                    cameraScale *= 1 - Config.scrollSpeedDelta
                } else if (event.deltaY < 0) {
                    cameraScale *= 1 + Config.scrollSpeedDelta
                }
            }

            setOnKeyPressed { event ->
                when (event.code) {
                    KeyCode.W ->
                        vy = 1.0
                    KeyCode.S ->
                        vy = -1.0
                    KeyCode.A ->
                        vx = 1.0
                    KeyCode.D ->
                        vx = -1.0
                    else -> return@setOnKeyPressed
                }
            }

            setOnKeyReleased { event ->
                when (event.code) {
                    KeyCode.A, KeyCode.D ->
                        vx = 0.0
                    KeyCode.W, KeyCode.S ->
                        vy = 0.0
                    else -> return@setOnKeyReleased
                }
            }
            start()
        }

        fun fullMapScale() {
            cameraScale = minOf(clipWidth / width, clipHeight / height)
        }

        fun start() {
            gameDirector.start()
        }

        fun stop() {
            gameDirector.stop = true
        }

        fun clipOffsetX() = (width - clipWidth / cameraScale) / 2 - translateX / cameraScale
        fun clipOffsetY() = (height - clipHeight / cameraScale) / 2 - translateY / cameraScale
        /**
         * ensure the canvas doesn't come out
         */
        private fun clipCanvas() {
            val clipRect = Rectangle(clipOffsetX(), clipOffsetY(), clipWidth / cameraScale, clipHeight / cameraScale)
            clip = clipRect
        }

        private fun nodeTranslation(node: Node): Affine {
            return Affine(Affine.translate(node.leftX, node.upperY))
        }

        private fun nodeRotation(node: Node): Affine {
            if (node is RobotNode) {
                return Affine(Affine.rotate(node.degreeOrientation + 90.0, node.width / 2, node.height / 2))
            } else {
                return Affine(Affine.rotate(node.degreeOrientation, node.width / 2, node.height / 2))
            }
        }

        private fun drawNode(node: Node, transform: Affine = Affine()) {
            val originalTrans = graphicsContext2D.transform
            val newTrans: Affine = transform
            newTrans.append(nodeTranslation(node))
            newTrans.append(nodeRotation(node))
            graphicsContext2D.transform = newTrans
            graphicsContext2D.drawImage(node.image, 0.0, 0.0, node.width, node.height)
            // draw focus if apply
            if (node.focusedByPlayer) {
                drawFocus(node)
            }
            // recursively draw its children, assign a copy to prevent concurrency problem
            val children = node.immutableChildren
            children.forEach {
                if (it is GuiNode) {
                    drawGuiNode(it, node)
                } else {
                    drawNode(it, newTrans.clone())
                }
            }
            graphicsContext2D.transform = originalTrans
        }

        private fun drawFocus(node: Node) {
            graphicsContext2D.transform = Affine()
            graphicsContext2D.strokeOval(node.leftX, node.upperY, node.width, node.height)
        }
    }
}