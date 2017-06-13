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
open class RobotModel(var name: String, val components: List<DefaultComponent>) : Model {
    override val width: Double
        get() = Config.botSize
    override val height: Double
        get() = Config.botSize
    @Suppress("SENSELESS_COMPARISON")
    @Transient final
    override val image: Image
        get() {
            if (field == null) {
                field = idleImage()
            }
            return field
        }
    val maxSpeed: Double
    val maxAcceleration: Double
    val turnSpeed: Double
    val health: Int
    val weight: Int
    val empty: Boolean
        get() = components.isEmpty()
    val immutableImage: Image
        get() {
            if (_immutableImage == null) {
                _immutableImage = immutableImage()
            }
            return _immutableImage!!
        }
    @Transient
    private var _immutableImage: Image? = null

    init {
        image = idleImage()
        var forceSum = 0.0
        var turnSum = 0.0
        var healthSum = 0
        var weightSum = 0
        for (comp in components.filter { it is MovementComponent }) {
            val component = (comp as MovementComponent)
            forceSum += component.model.force
            turnSum += component.model.turn
            healthSum += component.model.health
            weightSum += component.model.weight
        }
        maxSpeed = forceSum
        maxAcceleration = forceSum / 20
        turnSpeed = turnSum
        health = healthSum
        weight = weightSum
    }

    constructor() : this("", arrayListOf())

    private fun immutableImage(): WritableImage {
        val writeImage = WritableImage(Config.botPixelSize.toInt(), Config.botPixelSize.toInt())
        val writer = writeImage.pixelWriter
        components
                .filter { it !is WeaponComponent }
                .forEach { writer.setPixels(it.x.toInt(), it.y.toInt(), it.image) }
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
        components.filter { it is WeaponComponent }.forEach { writer.setPixels(it.x.toInt(), it.y.toInt(), it.image) }

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

}
