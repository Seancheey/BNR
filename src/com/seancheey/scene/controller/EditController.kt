package com.seancheey.scene.controller

import com.seancheey.game.*
import com.seancheey.game.model.ComponentModel
import com.seancheey.game.model.ComponentNode
import com.seancheey.game.model.RobotModel
import com.seancheey.gui.*
import com.seancheey.resources.Models
import com.seancheey.resources.Resources
import com.seancheey.scene.Scenes
import com.seancheey.scene.Stages
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.ClipboardContent
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.scene.layout.*
import java.net.URL
import java.util.*

/**
 * Created by Seancheey on 20/05/2017.
 * GitHub: https://github.com/Seancheey
 */


/**
 * This EditController is used as a JavaFX controller
 * One should not create any new instance of this class
 */
class EditController : Initializable, RobotEditInterface {
    override var symmetricBuild: Boolean
        get() = Config.player.setting.symmetricBuild
        set(value) {
            Config.player.setting.symmetricBuild = value
        }
    /**
     * change stack used to record all changes on current robot
     */
    override var editRobotModelStack: ArrayList<RobotModel> = arrayListOf()

    /**
     * update called each time when any change apply to editing robot
     */
    override fun updateRobotModel() {
        // sync name field
        if (nameField!!.text != editingRobot.name) {
            nameField!!.text = editingRobot.name
        }
        // remove not synced views and add not synced views
        val componentViewNodes = editPane!!.children.filterIsInstance<ComponentView>().map { it.component }
        componentViewNodes.filterNot { it in editingRobot.components }.forEach { removeComponentView(it) }
        editingRobot.components.filterNot { it in componentViewNodes }.forEach { addComponentView(it) }
        // sync with player's editing model
        Config.player.robotGroups[selectBotGroupIndex][selectBotModelIndex] = editingRobot
        // sync with robot slot image
        (botGroupBox!!.children[selectBotModelIndex] as ModelSlot).updateModel(editingRobot)
        // save player's data
        Config.player.saveData()
        // update stats pane
        val intStatLabels = statsPane!!.children.filterIsInstance<StatusLabel<Int>>()
        intStatLabels.forEach {
            if (it.statusValue is Int)
                it.statusValue =
                        if (it.statusName == "health") editingRobot.health
                        else if (it.statusName == "weight") editingRobot.weight
                        else if (it.statusName == "price") editingRobot.price
                        else -1
        }
        val doubleStatLabels = statsPane!!.children.filterIsInstance<StatusLabel<Double>>()
        doubleStatLabels.forEach {
            if (it.statusValue is Double)
                it.statusValue =
                        if (it.statusName == "maxSpeed") editingRobot.maxSpeed
                        else if (it.statusName == "acceleration") editingRobot.maxAcceleration
                        else if (it.statusName == "turn") editingRobot.turnSpeed
                        else -1.0
        }
        // update error message
        errorMessageBox!!.children.clear()
        val messages = editingRobot.verify()
        errorMessageBox!!.children.addAll(messages.map { WrongMessageLabel(it) })
        // update warning grid
        editPane!!.children.removeAll(editPane!!.children.filterIsInstance<WarningGrid>())
        messages.forEach { it.points.forEach { addWarningGrid(it) } }
    }

    /**
     * currently editing robot model
     * each time it changes, previous value is added to change stack
     */
    override var editingRobot: RobotModel = RobotModel()
        set(value) {
            editRobotModelStack.add(field)
            field = value
        }

    /** generated by javafx, each time the pane is initialized, editController too **/
    companion object {
        var editController: EditController? = null
        val NONE = -1
    }

    /**
     * ComponentNode panes for player to select component models
     */
    @FXML
    var blocksPane: TilePane? = null
    @FXML
    var weaponsPane: TilePane? = null
    @FXML
    var movementsPane: TilePane? = null
    /**
     * Pane for player to edit their ships
     */
    @FXML
    var editPane: AnchorPane? = null
    /**
     * TextField of robot name
     */
    @FXML
    var nameField: TextField? = null
    /**
     * RobotNode selection HBox
     */
    @FXML
    var botGroupBox: HBox? = null
    @FXML
    var statsPane: TilePane? = null
    /**
     * buttons of four directions
     */
    @FXML
    var upButton: Button? = null
    /**
     * buttons of four directions
     */
    @FXML
    var downButton: Button? = null
    /**
     * buttons of four directions
     */
    @FXML
    var leftButton: Button? = null
    /**
     * buttons of four directions
     */
    @FXML
    var rightButton: Button? = null
    /**
     * errorMessageBox for verify robot
     */
    @FXML
    var errorMessageBox: VBox? = null
    /**
     * root pane for recursively doing things
     */
    @FXML
    var rootPane: BorderPane? = null
    /**
     * toggle button for symmetric building
     */
    @FXML
    var symmetricBuildButton: ToggleButton? = null
    /**
     * Index of player's selected index of bot group and model
     */
    var selectBotGroupIndex: Int = 0
    var selectBotModelIndex: Int = EditController.NONE


    override fun initialize(location: URL?, resources: ResourceBundle?) {
        editController = this
        initModelSlotTab(blocksPane!!, Models.BLOCKS)
        initModelSlotTab(weaponsPane!!, Models.WEAPONS)
        initModelSlotTab(movementsPane!!, Models.MOVEMENTS)
        initEditPane()
        initBotGroup()
        initMovingButtons()
        initStatusPane()
        initKeyListener()
        initMouseListener()
        setEditingRobot(0)
        if (symmetricBuild) {
            symmetricBuildButton!!.selectedProperty().set(true)
        }
    }

    private fun initModelSlotTab(pane: TilePane, modelList: List<ComponentModel>) {
        for (model in modelList) {
            val modelSlot = ModelSlot(model)
            modelSlot.setOnDragDetected { event ->
                dragComponentStart(model, modelSlot)
                event.consume()
            }
            pane.children.add(modelSlot)
        }
    }

    private fun initEditPane() {
        val size = Config.botPixelSize
        editPane!!.minWidth = size
        editPane!!.maxWidth = size
        // add background arrow
        val backgroundArrow = ImageView(Image(Resources.arrowImageInStream, Config.botPixelSize, Config.botPixelSize, false, false))
        AnchorPane.setTopAnchor(backgroundArrow, 0.0)
        AnchorPane.setBottomAnchor(backgroundArrow, 0.0)
        AnchorPane.setLeftAnchor(backgroundArrow, 0.0)
        AnchorPane.setRightAnchor(backgroundArrow, 0.0)
        editPane!!.children.add(backgroundArrow)
        // add grid to edit pane
        val grids = arrayListOf<DragDropGrid>()
        for (y in 0 until Config.botGridNum) {
            for (x in 0 until Config.botGridNum) {
                val grid = DragDropGrid(x, y, { gridX, gridY, model ->
                    addComponentAt(gridX, gridY, model)
                    setAllMountComponentTransparent(false)
                })
                AnchorPane.setTopAnchor(grid, Config.botGridSize * y)
                AnchorPane.setLeftAnchor(grid, Config.botGridSize * x)
                grids.add(grid)
            }
        }
        editPane!!.children.addAll(grids)
        nameField!!.setMaxSize(size, size)
    }

    private fun initBotGroup() {
        // select player's first BotGroup to initialize
        val models = Config.player.robotGroups[0]
        models.mapIndexed { i, model -> RobotModelSlot(model, { setEditingRobot(i) }) }.forEach { botGroupBox!!.children.add(it) }
    }

    private fun initMovingButtons() {
        val size = 25.0
        val upImage = ImageView(Image(Resources.arrowImageInStream, size, size, false, false))
        upImage.rotate = 0.0
        val downImage = ImageView(Image(Resources.arrowImageInStream, size, size, false, false))
        downImage.rotate = 180.0
        val rightImage = ImageView(Image(Resources.arrowImageInStream, size, size, false, false))
        rightImage.rotate = 90.0
        val leftImage = ImageView(Image(Resources.arrowImageInStream, size, size, false, false))
        leftImage.rotate = 270.0
        upButton!!.graphic = upImage
        downButton!!.graphic = downImage
        rightButton!!.graphic = rightImage
        leftButton!!.graphic = leftImage
    }

    private fun initStatusPane() {
        statsPane!!.children.add(StatusLabel("price", 0))
        statsPane!!.children.add(StatusLabel("health", 0))
        statsPane!!.children.add(StatusLabel("weight", 0))
        statsPane!!.children.add(StatusLabel("maxSpeed", 0.0))
        statsPane!!.children.add(StatusLabel("acceleration", 0.0))
        statsPane!!.children.add(StatusLabel("turn", 0.0))
    }

    private fun initKeyListener() {
        rootPane!!.setOnKeyPressed { event ->
            when (event.code) {
                KeyCode.A ->
                    moveLeftButtonPressed()
                KeyCode.D ->
                    moveRightButtonPressed()
                KeyCode.W ->
                    moveUpButtonPressed()
                KeyCode.S ->
                    moveDownButtonPressed()
                else ->
                    return@setOnKeyPressed
            }
        }
    }

    private fun initMouseListener() {
        recursiveAddMouseListener(rootPane!!, {
            setAllMountComponentTransparent(false)
        })
    }


    private fun recursiveAddMouseListener(node: Node, action: (MouseEvent) -> Unit) {
        if (node is Region && node != nameField) {
            node.childrenUnmodifiable.forEach { recursiveAddMouseListener(it, action) }
            node.setOnMouseReleased(action)
        }
    }

    /**
     * set the current editing robot
     * @param index the index of editing robot
     */
    fun setEditingRobot(index: Int) {
        // set editing index
        selectBotModelIndex = index
        resetRobotModel(Config.player.robotGroups[selectBotGroupIndex][index])
        // set id for css
        for (node in botGroupBox!!.children) {
            node.id = ""
        }
        botGroupBox!!.children[index].id = "selectedRobotModel"
    }

    private fun addComponentView(component: ComponentNode) {
        // prevent overlapped component with same type
        if (editPane!!.children.filterIsInstance<ComponentView>().any { it.componentModel == component }) {
            return
        }
        val componentView = ComponentView(component.model, component.gridX, component.gridY, { event, compView ->
            dragComponentStart(compView.componentModel, compView)
            if (!event.isShiftDown) {
                removeComponent(component)
            }
        })
        AnchorPane.setLeftAnchor(componentView, component.leftX)
        AnchorPane.setTopAnchor(componentView, component.upperY)
        editPane!!.children.add(componentView)
    }

    private fun removeComponentView(node: ComponentNode) {
        editPane!!.children.removeAll(
                editPane!!.children.filterIsInstance<ComponentView>().firstOrNull { it.component == node }
        )
    }

    private fun addWarningGrid(point: RobotModel.Point) {
        val grid = WarningGrid()
        AnchorPane.setLeftAnchor(grid, point.x * Config.botGridSize)
        AnchorPane.setTopAnchor(grid, point.y * Config.botGridSize)
        editPane!!.children.add(grid)
    }

    fun dragComponentStart(componentModel: ComponentModel, node: Node): Unit {
        // start drag with any transfer mode
        val db = node.startDragAndDrop(TransferMode.COPY, TransferMode.LINK, TransferMode.MOVE)
        // put the componentModel into clipboard
        val clipboard = ClipboardContent()
        clipboard.put(modelCopyFormat, componentModel)
        db.setContent(clipboard)
        // set mouse holding image and offsets
        db.dragView = componentModel.image
        db.dragViewOffsetX = (componentModel.gridWidth - 1) * componentModel.image.width / componentModel.gridWidth / 2 - componentModel.xCorrect / 2
        db.dragViewOffsetY = -(componentModel.gridHeight - 1) * componentModel.image.height / componentModel.gridHeight / 2 + componentModel.yCorrect / 2
        setAllMountComponentTransparent(true)
    }

    fun setAllMountComponentTransparent(value: Boolean) {
        editPane!!.children.filterIsInstance<ComponentView>().
                filter { Attribute.weapon_mount in it.componentModel.attributes }.
                forEach {
                    it.isMouseTransparent = value
                }
    }

    fun nameFieldSyncName() {
        setRobotModelName(nameField!!.text)
    }

    fun saveButtonPressed() {
        Config.player.saveData()
    }

    fun menuButtonPressed() {
        Stages.switchScene(Scenes.menu)
    }

    fun battleButtonPressed() {
        Stages.switchScene(Scenes.battle, 1080.0, 670.0)
    }

    fun moveUpButtonPressed() {
        moveAllComponents(0, -1)
    }

    fun moveDownButtonPressed() {
        moveAllComponents(0, 1)
    }

    fun moveRightButtonPressed() {
        moveAllComponents(1, 0)
    }

    fun moveLeftButtonPressed() {
        moveAllComponents(-1, 0)
    }

    fun undoButtonPressed() {
        undoRobotModel()
    }

    fun symmetricButtonPressed() {
        symmetricBuild = symmetricBuildButton!!.isSelected
    }
}

