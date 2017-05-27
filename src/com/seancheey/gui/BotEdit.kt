package com.seancheey.gui

import com.seancheey.game.GameConfig
import com.seancheey.game.Model
import com.seancheey.game.Models
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.input.*
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.StackPane
import java.net.URL
import java.util.*

/**
 * Created by Seancheey on 20/05/2017.
 * GitHub: https://github.com/Seancheey
 */

object modelFormat : DataFormat("object/model")

private var editController: BotEdit? = null

private val gridWidth: Double = GameConfig.edit_grid_width.toDouble()
private val gridNum: Int = GameConfig.edit_grid_num

class BotEdit : Initializable {
    @FXML
    var borderPane: BorderPane? = null
    @FXML
    var blocksFlowPane: FlowPane? = null
    @FXML
    var weaponsFlowPane: FlowPane? = null
    @FXML
    var editPane: AnchorPane? = null
    @FXML
    var nameField: TextField? = null

    init {
        editController = this
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        initModelFlowPanes()
        initEditPane()
    }

    private fun initModelFlowPanes(): Unit {
        for (component in Models.blocks) {
            blocksFlowPane!!.children.add(ModelSlot(component))
        }
    }

    private fun initEditPane(): Unit {
        val size = gridNum * gridWidth
        editPane!!.minWidth = size
        editPane!!.maxWidth = size
        // add grid to edit pane
        val grids = arrayListOf<ComponentGrid>()
        for (y in 0..gridNum - 1) {
            for (x in 0..gridNum - 1) {
                val grid = ComponentGrid(x, y)
                AnchorPane.setTopAnchor(grid, gridWidth * y)
                AnchorPane.setLeftAnchor(grid, gridWidth * x)
                grids.add(grid)
            }
        }
        editPane!!.children.addAll(grids)
        nameField!!.setMaxSize(size, size)
    }

    fun putComponent(model: Model, x: Int, y: Int): Unit {
        putComponentView(model, x, y)
        switchComponentGridInRange(x, y, model.width, model.height, false)
    }

    fun switchComponentGridInRange(x: Int, y: Int, width: Int, height: Int, value: Boolean) {
        for (y2 in y..y + height - 1) {
            for (x2 in x..x + width - 1) {
                val compGrid = getComponentGridAt(x2, y2)
                if (compGrid != null) {
                    compGrid.enabled = value
                }
            }
        }
    }

    private fun putComponentView(model: Model, x: Int, y: Int) {
        val componentView = ComponentView(model, x, y)
        AnchorPane.setLeftAnchor(componentView, x * gridWidth)
        AnchorPane.setTopAnchor(componentView, y * gridWidth)
        editPane!!.children.add(componentView)
    }

    private fun getComponentGridAt(x: Int, y: Int): ComponentGrid? {
        if (x < gridWidth || y < gridWidth) {
            for (node in editPane!!.children) {
                if (node is ComponentGrid) {
                    if (node.x == x && node.y == y) {
                        return node
                    }
                }
            }
        }
        return null
    }
}

class ComponentView(val model: Model, val x: Int, val y: Int) : ImageView(model.image) {
    init {
        fitWidth = GameConfig.edit_grid_width * model.width.toDouble()
        fitHeight = GameConfig.edit_grid_width * model.height.toDouble()
        setOnDragDetected { event ->
            dragComponentStart(model, this, event)
            editController!!.editPane!!.children.remove(this)
            editController!!.switchComponentGridInRange(x, y, model.width, model.height, true)
        }
    }
}

class ComponentGrid(val x: Int, val y: Int, model: Model? = null) : StackPane() {

    var model: Model? = null
    var enabled = true

    init {
        this.model = model

        minWidth = gridWidth
        minHeight = gridWidth
        maxWidth = gridWidth
        maxHeight = gridWidth

        setOnDragOver {
            event ->
            if (enabled) {
                event.acceptTransferModes(TransferMode.MOVE, TransferMode.LINK, TransferMode.COPY)
                event.consume()
            }
        }
        setOnDragDropped { event ->
            if (enabled) dragComponentEnd(x, y, event)
        }

    }
}

class ModelSlot(val componentModel: Model) : ImageView(componentModel.imageURL) {

    init {
        id = "model_slot"
        if (image.height > 50 || image.width > 50) {
            fitWidth = 50.0
            fitHeight = 50.0
        }
        // bind mouse location to hoverView
        setOnDragDetected { event ->
            dragComponentStart(componentModel, this, event)
        }
    }
}

fun dragComponentStart(model: Model, node: Node, event: MouseEvent): Unit {
    // start drag with any transfer mode
    val db = node.startDragAndDrop(TransferMode.COPY, TransferMode.LINK, TransferMode.MOVE)
    // put the model into clipboard
    val clipboard = ClipboardContent()
    clipboard.put(modelFormat, model)
    db.setContent(clipboard)
    // set mouse holding image
    db.dragView = model.image
    event.consume()
}

fun dragComponentEnd(x: Int, y: Int, event: DragEvent) {
    if (event.dragboard.hasContent(modelFormat)) {
        val model = event.dragboard.getContent(modelFormat) as Model

        editController!!.putComponent(model, x, y)

        event.isDropCompleted = true
        event.consume()
    }
}

