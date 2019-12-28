package ui

import scalafx.collections.ObservableBuffer
import scalafx.scene.control._
import scalafx.scene.control.Button
import scalafx.scene.layout.HBox
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.Insets

@sfxml
class settingsController(favRestaurantCombo: ComboBox[String],
                         dietButtons: HBox,
                         highlightField: TextField,
                         highlightListView: ListView[String])
    extends ControllerInterface {

  private val lunchList = Main.lunchList
  // Mutable variables for keeping track of what's shown on the UI
  private var activatedDiets: Array[String] = Array()
  private var highlightedIngredients: Array[String] = Array()

  override def refreshContents(): Unit = {
    val settings = lunchList.settings
    // Set the mutable variables
    activatedDiets = settings.diets
    highlightedIngredients = settings.highlighted

    // Get every restaurants' names with favorite's name first
    val fav = settings.favRestaurantName
    val restaurants = lunchList.restaurants
      .map(restaurant => restaurant.name)
      .filter(_ != fav)
      .+:(fav)

    // Set restaurants to be displayed
    favRestaurantCombo.items = ObservableBuffer(restaurants: _*)
    favRestaurantCombo.getSelectionModel.selectFirst()

    // Set highlighted words to be displayed
    highlightListView.items = ObservableBuffer(highlightedIngredients: _*)


    // Get diets
    val allDiets = lunchList.diets
    // Set diets to be displayed by their actual names that can be gotten from Lunchlist
    dietButtons.children = allDiets.keys.map(dietCode => {
      val btn = new Button(allDiets(dietCode))
      // If the diet is activated, let's set it's style class to be active
      btn.styleClass =
        if (activatedDiets.contains(dietCode)) Iterable("btn", "btnActive")
        else Iterable("btn")
      // Set paddings
      btn.setPadding(Insets(5, 5, 5, 5))
      // Set toggling of diets as an action for the button
      btn.onAction = (e: ActionEvent) => {
        handleDietToggle(dietCode, btn)
      }
      btn
    })
  }

  // Event handler for the button that adds a new ingredient
  def handleAddHLIngredient(event: ActionEvent): Unit = {
    val value = highlightField.getText
    // Here's the infamous length constraint mentioned in the general plan :))
    if (value.length != 0 && value.length <= 64) {
      highlightedIngredients = highlightedIngredients.:+(value)
      highlightListView.getItems.add(value)
      highlightListView.getSelectionModel.selectLast()
    }
    highlightField.text = ""
  }

  // Event handler for the button that removes some ingredient
  def handleRemoveHLIngredient(event: ActionEvent): Unit = {
    // Get selected item
    val value = highlightListView.getSelectionModel.getSelectedItem
    val idx = highlightListView.getSelectionModel.getSelectedIndex
    // If there was an item selected and the idx fits the data we currently have
    if (idx != -1 && idx < highlightListView.getItems.size) {
      // Filter out the selected one
      highlightedIngredients = highlightedIngredients.filter(_ != value)
      // Remove it from the ui
      highlightListView.getItems.remove(idx)
      // Select the first element
      highlightListView.getSelectionModel.selectFirst()
    }
  }

  // Event handler for diet toggle
  def handleDietToggle(code: String, btn: Button): Unit = {
    // If active > deactivate, if deactive > activate
    if (activatedDiets.contains(code)) {
      btn.styleClass = Iterable("btn")
      activatedDiets = activatedDiets.filter(_ != code)
    } else {
      btn.styleClass = Iterable("btn", "btnActive")
      activatedDiets = activatedDiets.:+(code)
    }
  }

  // Handle's going back to the main view
  def handleBack(event: ActionEvent) {
    // Update settings to be their counterparts from this class
    lunchList.settings.favRestaurantName = favRestaurantCombo.getValue
    lunchList.settings.highlighted = highlightedIngredients
    lunchList.settings.diets = activatedDiets
    // Save settings into file
    lunchList.settings.save()
    // Switch view
    Main.switchScene("main")
  }
}
