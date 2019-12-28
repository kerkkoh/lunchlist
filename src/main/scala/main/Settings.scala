package main

import java.io.FileNotFoundException

import play.api.libs.json.{JsArray, JsResultException, JsValue, Json}
import com.fasterxml.jackson.core.JsonProcessingException

import reflect.io.File
import scala.io.Codec
import scala.io.Source.fromFile

class Settings(var diets: Array[String] = Array(),
               var favRestaurantName: String = "",
               var highlighted: Array[String] = Array()) {

  def load(): Unit = {
    // Takes in a JsValue and transforms it to an array of strings. Basically parses JSON arrays to scala arrays
    def processJsStringArray(jsValue: JsValue): Array[String] = {
      jsValue.asOpt[JsArray] // Try to cast it to JsArray
        .getOrElse(JsArray())
        .value // Cast the possible array to a List-type format or pass on an empty array
        .map(f => f.as[String])
        .toArray // Change array elements to strings and make sure we get an array
    }
    try {
      // Try to read the settings.json file and insert the properties into their own variables within the class
      val jsonData = Json.parse(fromFile("json/settings.json")("UTF8").mkString)
      favRestaurantName = (jsonData \ "fav").get
        .asOpt[String] // Try to cast it to String
        .getOrElse("") // If we couldn't do that, let's just assume there's no favorite restaurant
      diets = processJsStringArray((jsonData \ "diets").get)
      highlighted = processJsStringArray((jsonData \ "highlighted").get)
    } catch {
      // Settings file doesn't exist, let's make one
      case _: FileNotFoundException =>
        save()
        load()
      // Faulty JSON format (couldn't parse or couldn't get some required property)
      case e @ (_: JsonProcessingException | _: JsResultException) =>
        println(s"Faulty JSON format in json/settings.json. ${e.getMessage}")
      // Print other errors
      case e: Exception => e.printStackTrace()
    }
  }

  def save(): Unit = {
    // Save the settings into a json file
    File("json/settings.json")(Codec.UTF8).writeAll(
      Json.stringify(
        Json.obj(
          "fav" -> favRestaurantName,
          "diets" -> diets,
          "highlighted" -> highlighted
        )
      )
    )
  }
}
