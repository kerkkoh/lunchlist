package main

import java.io.FileNotFoundException
import java.nio.charset.MalformedInputException
import java.time.LocalDate

import com.fasterxml.jackson.core.JsonProcessingException
import play.api.libs.json.{JsArray, JsResultException, Json}

import scala.io.Codec
import scala.io.Source.fromFile
import scala.reflect.io.File

class Restaurant(val name: String,
                 private var knownDays: Map[LocalDate, Option[Menu]],
                 api: ListAPI,
                 apiKey: String,
                 openingHours: Array[String]) {

  // Two alternative constructors
  def this(name: String, api: ListAPI, apiKey: String) {
    this(name, Map(), api, apiKey, Array())
  }
  def this(name: String,
           api: ListAPI,
           apiKey: String,
           openingHours: Array[String]) {
    this(name, Map(), api, apiKey, openingHours)
  }

  // File for saving lists is lowercase name of the restaurant without spaces with a suffix "-savedLists.json"
  // and it's in the folder 'json'
  private val savedListsFileName = "json/" + name
    .filter(_ != ' ')
    .toLowerCase + "-savedLists.json"

  // Saves the knownDays into savedListsFileName
  private def saveLists(): Unit = {
    try {
      File(savedListsFileName)(Codec.UTF8).writeAll(
        // Stringify an array of JsValues
        Json.stringify(
          // Transfer current knownDays array of maps into a JsArray of JsObjects also filtering out all 'None' values
          JsArray(
            knownDays
              .filter(day => day._2.isDefined)
              .map(day => {
                val menuOpt = day._2
                val (body, lunchTime) = if (menuOpt.isDefined) {
                  val menu = menuOpt.get
                  // Map lines from body into JsObjects so they can be saved
                  val tempBody = menu.body.map(line =>
                    Json.obj(
                      "name" -> line.name, // Object should contain the name property of the line
                      "diets" -> line.diets, // Should also contain the diets property, if it's something and not None
                      "type" -> line.getClass.getSimpleName.toLowerCase // This makes sure when we load it back into the program, we know what kind of a Line it was
                  ))

                  // Give out the body and lunchTime
                  (Some(tempBody), Some(menu.lunchTime))
                } else {
                  (None, None)
                }
                // The JSON parser needs a json object so we need to create one
                Json.obj(
                  "date" -> day._1.toString, // The object should have the date, as a string
                  "body" -> body, // Should also have the body, which is now an array of json objects
                  "lunchTime" -> lunchTime // Should have the lunchTime
                )
              })
              .toArray
          )
        )
      )
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
  // Loads (relevant) saved lists from file savedLists.json
  private def loadLists(): Map[LocalDate, Option[Menu]] = {
    try {
      // Get the json contents of the file as an array
      val jsonString = fromFile(savedListsFileName)("UTF8").mkString
      val jsonArray = Json.parse(jsonString).as[JsArray]

      // Filter the array of saved lists so that it doesn't include old lists (purge old ones)
      val purgedJsonArray = jsonArray.value.filter(list => {
        // Parse out date
        val date = LocalDate.parse((list \ "date").as[String])
        // If date is before today, let's not include it in the file anymore
        !date.isBefore(LocalDate.now)
      })

      // Write the purged list back to the savedLists.json file
      File(savedListsFileName).writeAll(
        Json.stringify(
          JsArray(purgedJsonArray)
        )
      )

      // Return an array of lists that were in the file to be added in the knownLists
      purgedJsonArray
        .map(list => {
          // Use LocalDate to parse it out of the JSON
          val date = LocalDate.parse((list \ "date").as[String])
          // Body needs to be parsed into an array of Line objects, if it isn't null, which it shouldn't be
          val bodyOpt = (list \ "body").asOpt[JsArray]
          if (bodyOpt.isDefined) {
            val body: Array[Line] = bodyOpt.get.value
              .map(jsObject => { // Map the JSON objects to Line instances (Dish/Title)
                val name = (jsObject \ "name").as[String]
                // This determines whether to use Dish or Title case class
                val dishOrTitle = (jsObject \ "type").as[String]
                // If diets aren't null, we transform the diets into Strings
                val diets = (jsObject \ "diets")
                  .asOpt[JsArray]
                  .map(jsArray => jsArray.value.map(f => f.as[String]).toArray)

                if (dishOrTitle == "title") Title(name) else Dish(name, diets)
              })
              .toArray

            val lunchTime = (list \ "lunchTime").asOpt[String]
            date -> Some(Menu(body, lunchTime))
          } else {
            date -> None
          }
        })
        .toMap
    } catch {
      // File doesn't exist, let's make one with the knownDays provided to the restaurant class
      case _: FileNotFoundException =>
        saveLists()
        // Return nothing, as there was nothing in the file ( it didn't exit )
        Map()
      // Faulty JSON format (couldn't parse or couldn't get some required property)
      case e @ (_: JsonProcessingException | _: JsResultException) =>
        println(s"Faulty JSON format in " + savedListsFileName + ". ${e.getMessage}")
        Map()
      // This handles an error where the program was executed on different platforms, which sometimes can cause
      // the file encoding for these files to become strange.
      case e: MalformedInputException =>
        Map()
      // Print other errors
      case e: Exception =>
        e.printStackTrace()
        Map()
    }
  }

  // Use the loadLists method to get the relevant lists into memory from the file
  knownDays = knownDays ++ loadLists()

  /* Gets the list from API/memory
   *                                                 Returned tuple:
   *                                                (name,   menu)
   */
  def getList(date: LocalDate, settings: Settings): (String, Menu) = {
    // Find menu from memory or if it isn't in memory, fetch it from the API
    val possibleMenu: Option[Menu] =
      if (knownDays.isDefinedAt(date))
        knownDays(date)
      else {
        val fetchedMenu = api.fetch(date, apiKey)
        knownDays = knownDays + (date -> fetchedMenu)
        // Save lists to file
        saveLists()
        fetchedMenu
      }

    // If possibleMenu has been defined, let's process the list according to the user's settings,
    // otherwise the restaurant is closed or the list isn't available.
    if (possibleMenu.isDefined) {
      val menu = possibleMenu.get
      // Get the lunchtime provided by the api or if there isn't one, try the openingHours array and lastly default to "Unknown"
      val lunchTime: String = menu.lunchTime.getOrElse({
        val index = date.getDayOfWeek.getValue - 1
        if (openingHours.isDefinedAt(index))
          openingHours(index)
        else "Unknown"
      })

      // Filter dishes that don't fit the diets set in settings.
      val body = menu.body.filter(
        line =>
          // If the line is a title, we don't filter it. If it is a dish and the diets doesn't contain our special diet
          // we filter it from the menu as the user can not eat that.
          if (line.getClass.getSimpleName == "Dish") {
            settings.diets.forall(
              diet =>
                line.diets
                  .getOrElse(Array())
                  .exists(lineDiet => diet.toLowerCase == lineDiet.toLowerCase))
          } else true
      )

      (name, Menu(body, Some(lunchTime)))
      // There wasn't a menu in memory nor did the api come back with anything useful so we assume the restaurant is closed
    } else (name, Menu(Array(Title("List not available")), None))
  }
}
