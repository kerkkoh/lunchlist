package ui

import java.time.{DayOfWeek, LocalDate}
import java.util.Locale

import scalafx.scene.control.{Button, Label}
import scalafx.scene.layout.{HBox, VBox}
import scalafxml.core.macros.sfxml
import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.geometry.Insets
import scalafx.scene.effect.DropShadow
import scalafx.scene.paint.Color

import scala.collection.breakOut

@sfxml
class mainController(private val listsBox: HBox, private val buttons: HBox)
    extends ControllerInterface {

  private val lunchList = Main.lunchList

  var curShownDate: LocalDate = LocalDate.now()

  override def refreshContents(): Unit = {
    // Get lists
    val lists = lunchList.getLists(curShownDate)
    // Get settings
    val settings = lunchList.settings
    // Turn the lists into a VBox, that contains labels
    val listNodes: Iterable[VBox] = lists.map(list => {
      val isFavorite: Boolean = list._1 == lunchList.settings.favRestaurantName

      // Make a new label for the restaurant's name and the lunchtime after it
      val title = new Label(list._1 + " (" + list._2.lunchTime.getOrElse("Closed") + ")\n")

      // Set style class for favorite restaurant
      title.styleClass =
        if (isFavorite)
          Iterable("restaurantName", "favoriteRestaurant")
        else
          Iterable("restaurantName")

      val body = list._2.body
      // Highlight the (Dish) lines that contain strings that the user wants highlighted
        .flatMap(line => {
          // Only for the Dish class, not the Title so return Title as is.
          if (line.getClass.getSimpleName == "Dish") {
            // If for every user specified highlighted word it's true that
            // the word doesn't appear in the "line's name" (dish name)
            val text =
              if (line.getClass.getSimpleName == "Title")
                line.name
              else if (line.diets.isDefined)
                line.name + " (" + line.diets.get.mkString(", ") + ")"
              else
                line.name

            // Create the actual label
            val lbl = Label(s"- $text")

            // Check if found at least one thing the user wants highlighted...
            val shouldHighlight = settings.highlighted.exists(highlightedWord =>
              line.name.toLowerCase.contains(highlightedWord.toLowerCase)
            )
            // ...so let's highlight the line if so
            if (shouldHighlight)
              lbl.styleClass = Iterable("highlighted")

            lbl.wrapText = true
            // No padding needed for these labels
            lbl.setPadding(Insets.Empty)
            Array(lbl)
          } else {
            val title = Label(line.name)
            // If this title isn't the first thing in the menu's body, we should add a little padding to it
            if (list._2.body.head.name != line.name)
              title.setPadding(Insets(10, 0, 0, 0))
            title.wrapText = true
            Array(title)
          }
        })

      // Contents of a VBox should be the body of the menu + the title prepended
      val wrapper = new VBox(body.+:(title): _*)
      wrapper.styleClass = Iterable("listbg")
      wrapper.padding = Insets(5, 5, 5, 5)
      wrapper
    })
    listsBox.children = listNodes
  }

  // Map that can be used for getting the DayOfWeek object with a short weekday name like "Mon"
  private val days: Map[String, DayOfWeek] = DayOfWeek
    // Contains all DayOfWeek objects
    .values()
    // Map them to be just their short name
    .map(_.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH))
    // Zip them with themselves and break them out to a map
    .zip(DayOfWeek.values())(breakOut): Map[String, DayOfWeek]

  // Gets all days that are left in the current week, including the current one
  private def getDays: Array[String] = {
    // Get all weekdays
    val allDays = DayOfWeek.values()
    val today = LocalDate.now().getDayOfWeek
    // Drop days that have passed and maps the rest to their short names like "Mon"
    allDays
      .dropWhile(day => day.getValue < today.getValue)
      .map(_.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH))
  }

  // Get days in a button form, so maps getDays to Buttons, in a nutshell
  def dayButtons: Array[Button] =
    getDays.map(dayStr => {
      val btn = new Button(dayStr)
      // Add action for changing of days in the UI
      btn.onAction = (event: ActionEvent) => {
        handleDayChange(dayStr)
      }
      // Add in some cool shadow on it
      btn.effect = new DropShadow(5.0, 4.0, 4.0, Color.web("#c82ccd"))
      if (days(dayStr) == curShownDate.getDayOfWeek) {
        btn.styleClass = Iterable("active")
      }
      btn
    })
  buttons.children = dayButtons

  // Handles the switch to the settings scene
  def handleSettings(event: ActionEvent) {
    Main.switchScene("settings")
  }

  // Handles a button press of a day, so that we change a day refresh the contents
  def handleDayChange(dayStr: String): Unit = {
    // Get the day we're changing to
    val weekd = days(dayStr)
    val weekdNbr = weekd.getValue
    // Today's number
    val todayNbr = LocalDate.now.getDayOfWeek.getValue
    // Difference between them
    val diff = weekdNbr - todayNbr
    // Add to the current shown date the difference in days
    curShownDate = LocalDate.now.plusDays(diff)
    // Refresh the buttons
    buttons.children = dayButtons

    // Refresh the contents
    refreshContents()
  }
}
