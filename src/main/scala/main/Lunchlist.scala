package main

import play.api.libs.json.{JsArray, JsResultException, JsValue, Json}
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.FileNotFoundException

import scala.collection.breakOut
import scala.io.Source.fromFile
import java.time.LocalDate

import scala.io.Codec

// Lunchlist has default values for restaurants, settings and diets
class Lunchlist(val settings: Settings = new Settings(Array(), "Alvari", Array()),
                // Allowed diets and their descriptions
                val diets: Map[String, String] = Map("L" -> "Lactose free",
                                                     "VL" -> "Low lactose",
                                                     "G" -> "Gluten free",
                                                     "M" -> "Milk free",
                                                     "Veg" -> "Vegan",
                                                     "*" -> "Well being")) {

  /*
      restaurants.json is a json array filled with objects that represent restaurants:

      Format:
      Field         - (type)   - Description

    {
      name          - (String) - Restaurant name
      api           - (String) - API object's name (Currently available apis can be found in 'availableAPIs' and 'apiObjects')
      apiKey        - (String) - API key / Restaurant's id in the api
      openingHours  - (Array)  - If the restaurant's API doesn't provide opening hours (This is the case with sodexo restaurants), this 7 element array has opening hours for each day
    }
   */
  private val defaultRestaurants: String =
    """[{"name":"Alvari","api":"FazerAPI","apiKey":"0190","openingHours":[]},{"name":"A Bloc","api":"FazerAPI","apiKey":"3087","openingHours":[]},{"name":"Valimo","api":"SodexoAPI","apiKey":"13918","openingHours":["10.00 - 14.30","10.00 - 14.30","10.00 - 14.30","10.00 - 14.30","10.00 - 14.00","Closed","Closed"]},{"name":"Kvarkki","api":"SodexoAPI","apiKey":"26521","openingHours":["08.00 - 14.00","08.00 - 14.00","08.00 - 14.00","08.00 - 14.00","08.00 - 14.00","Closed","Closed"]}]"""
  private val availableAPIs: Array[ListAPI] = Array(FazerAPI, SodexoAPI)
  private val apiObjects: Map[String, ListAPI] = (
    availableAPIs.map(_.getClass.getSimpleName.dropRight(1)) zip availableAPIs
    )(breakOut): Map[String, ListAPI]

  private def checkRestaurantSyntaxValidity(js: JsValue): Boolean = {
    // These validate the inputs via the JSON parser and return either Success or Error wrappers
    val name = (js \ "name").validate[String]
    val api = (js \ "api").validate[String]
    val apiKey = (js \ "apiKey").validate[String]
    val openingHours = (js \ "openingHours").validate[Array[String]]

    // If we found an api entry, check if it's actually an API we support
    val validAPI = if (api.isSuccess) {
      apiObjects.isDefinedAt(api.get)
    } else false

    // If everything checks out, we can safely say that the syntax was valid :)
    if (name.isSuccess && apiKey.isSuccess && openingHours.isSuccess && validAPI) {
      true
    } else {
      // And if it doesn't check out, let's make sure the user knows about that and how to fix the problem
      println("Encountered faulty restaurant object in restaurants.json: " + Json.stringify(js))
      if (!name.isSuccess)
        println("Couldn't resolve for a required field 'name'")
      if (!apiKey.isSuccess)
        println("Couldn't resolve for a required field 'apiKey'")
      if (!openingHours.isSuccess)
        println("Couldn't resolve for a required field 'openingHours'")
      if (!validAPI)
        println("Available APIs are " + apiObjects.keys.mkString(", "))
      false
    }
  }
  private def restaurantsJsonStringToArray(jsonString: String): Array[Restaurant] = {
    val restaurants = Json.parse(jsonString).as[JsArray].value

    // Filter through json objects that depict restaurants and only choose valid ones
    // Hence, the program survives even faulty restaurants
    restaurants
      .filter(r => checkRestaurantSyntaxValidity(r))
      // Transform the JsValues of the restaurants into Restaurant instances
      .map(r => {
        val name = (r \ "name").as[String]
        val apiKey = (r \ "apiKey").as[String]
        val openingHours = (r \ "openingHours").as[Array[String]]

        // Take in the string representation of the api and return the api object via the apiObjects Map[String, ListAPI]
        val api = apiObjects((r \ "api").as[String])

        new Restaurant(name, api, apiKey, openingHours)
      })
      .toArray
  }

  val restaurants: Array[Restaurant] = try {
    // Try to read the restaurants.json
    val restaurantsString = fromFile("json/restaurants.json")("UTF-8").mkString

    // Return the array full of Restaurant instances
    restaurantsJsonStringToArray(restaurantsString)
  } catch {
    // Restaurants file doesn't exist, let's make the default one
    case _: FileNotFoundException => {
      reflect.io.File("json/restaurants.json")(Codec.UTF8)
        .writeAll(defaultRestaurants)
      // Return the array full of Restaurant instances.
      // Technically there's a slight problem if the defaultRestaurants string itself is problematic,
      // but since it's hardcoded by me, that's not gonna be a problem, hopefully
      restaurantsJsonStringToArray(defaultRestaurants)
    }
    // On other errors, we Initialize the program with no restaurants
    // Faulty JSON format (couldn't parse or couldn't get some required property)
    case e @ (_: JsonProcessingException | _: JsResultException) =>
      println(s"Faulty JSON format in json/restaurants.json. ${e.getMessage}")
      // Fall back to default restaurants
      restaurantsJsonStringToArray(defaultRestaurants)
    // Print other errors
    case e: Exception =>
      println(
        s"Unknown error while initializing restaurants from json/restaurants.json ${e.getMessage}")
      e.printStackTrace()
      Array()
  }

  // Initialize the settings object with default values or values from saved settings file
  def init(): Unit = settings.load()

  /* Gets all the lists in string form from restaurants, ready to be displayed in the UI
   *                                   Returned array of tuples:
   *                                   (name, list, lunchTime)
   */
  def getLists(date: LocalDate): Array[(String, Menu)] = {
    restaurants.map(_.getList(date, settings))
  }
}
