package com.seancheey.game

import com.seancheey.resources.Resources
import javafx.scene.image.Image
import javafx.scene.image.PixelWriter
import javafx.scene.image.WritableImage


/**
 * Created by Seancheey on 23/05/2017.
 * GitHub: https://github.com/Seancheey
 */
/**
 * Designed as an immutable class as robot model
 */
open class RobotModel(var name: String, val components: List<ComponentNode>) : Model {
    override val width: Double
        get() = Config.botPixelSize
    override val height: Double
        get() = Config.botPixelSize
    @Suppress("SENSELESS_COMPARISON")
    @Transient final
    override val image: Image = immutableImage()
        get() {
            if (field == null) {
                field = immutableImage()
            }
            return field
        }
    @Suppress("SENSELESS_COMPARISON")
    @Transient
    val idleImage: Image = idleImage()
        get() {
            if (field == null) {
                field = idleImage()
            }
            return field
        }
    val price: Int
    val maxSpeed: Double
    val maxAcceleration: Double
    val turnSpeed: Double
    val health: Int
    val weight: Int
    val empty: Boolean
        get() = components.isEmpty()

    init {
        val movementModels = components.filter { it.type == ComponentType.movement }.map { it.getModel<MovementModel>()!! }
        val allModels = components.map { it.model }

        health = allModels.sumBy { it.health }
        weight = allModels.sumBy { it.weight }
        price = allModels.sumBy { it.price }
        maxSpeed = movementModels.sumByDouble { it.force }
        maxAcceleration = movementModels.sumByDouble { it.force } / 20
        turnSpeed = movementModels.sumByDouble { it.turn }
    }

    constructor() : this("", arrayListOf())

    private fun immutableImage(): WritableImage {
        val writeImage = WritableImage(Config.botPixelSize.toInt(), Config.botPixelSize.toInt())
        val writer = writeImage.pixelWriter
        components
                .filter { it.type != ComponentType.weapon }
                .forEach { writer.setPixels(it.leftX.toInt(), it.upperY.toInt(), it.image) }
        return writeImage
    }

    /**
     * write a image to pixel writer
     */
    private fun PixelWriter.setPixels(x: Int, y: Int, image: Image) {
        val reader = image.pixelReader
        for (readY in 0 until image.height.toInt()) {
            for (readX in 0 until image.width.toInt()) {
                val color = reader.getColor(readX, readY)
                if (color.isOpaque) {
                    val xPos = x + readX
                    val yPos = y + readY
                    setColor(xPos, yPos, color)
                }
            }
        }
    }

    private fun idleImage(): Image {
        // return "no robot" image if there is no components in robot
        if (components.isEmpty()) {
            return Image(Resources.noRobotImageInStream)
        }
        // add all moving nodes to immutableImage
        val writableImage = immutableImage()
        val writer = writableImage.pixelWriter
        components.filter { it.type != ComponentType.weapon }.forEach { writer.setPixels(it.leftX.toInt(), it.upperY.toInt(), it.image) }

        return writableImage
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RobotModel) return false

        if (name != other.name) return false
        if (components != other.components) return false
        if (maxSpeed != other.maxSpeed) return false
        if (maxAcceleration != other.maxAcceleration) return false
        if (turnSpeed != other.turnSpeed) return false
        if (health != other.health) return false
        if (weight != other.weight) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + components.hashCode()
        result = 31 * result + maxSpeed.hashCode()
        result = 31 * result + maxAcceleration.hashCode()
        result = 31 * result + turnSpeed.hashCode()
        result = 31 * result + health
        result = 31 * result + weight
        return result
    }

    fun verify(): ArrayList<WrongMessage> {
        return Verifier().verify()
    }

    data class Point(val x: Int, val y: Int)
    data class WrongMessage(val message: String, val points: ArrayList<Point> = arrayListOf())

    private inner class Verifier {
        private val verifyList: ArrayList<() -> WrongMessage?> = arrayListOf()

        init {
            // verify movable
            verifyList.add {
                if (components.any { it.type == ComponentType.movement }) null else WrongMessage("The robot can't move! put some movement components")
            }
            // verify overlap
            verifyList.add {
                val overlapPoints: ArrayList<Point> = arrayListOf()
                val pointMap: HashMap<Point, ComponentNode> = hashMapOf()
                for (comp in components) {
                    val point = Point(comp.gridX, comp.gridY)
                    if (pointMap.containsKey(point)) {
                        // except for weapon & mount
                        if (comp.model.attributes.contains(Attribute.weapon_mount) && pointMap[point]!!.model is WeaponModel) {
                            continue
                        }
                        if (pointMap[point]!!.model.attributes.contains(Attribute.weapon_mount) && comp.model is WeaponModel) {
                            continue
                        }
                        if (!overlapPoints.contains(point))
                            overlapPoints.add(point)
                    } else {
                        pointMap.put(point, comp)
                    }
                }
                if (overlapPoints.isEmpty()) null else WrongMessage("Overlapped components found", overlapPoints)
            }
        }

        fun verify(): ArrayList<WrongMessage> {
            val messages = arrayListOf<WrongMessage>()
            verifyList.mapNotNullTo(messages) { it() }
            return messages
        }
    }
}
