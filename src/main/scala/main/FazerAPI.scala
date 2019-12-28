package main

import java.time.LocalDate

import play.api.libs.json.{JsArray, Json, JsResultException}
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.FileNotFoundException
import java.net.UnknownHostException

import scala.io.Source.fromURL

object FazerAPI extends ListAPI {
  override def fetch(date: LocalDate, apiKey: String): Option[Menu] = {
    val url: String = "https://www.fazerfoodco.fi/modules/json/json/Index?costNumber=" + apiKey + "&language=en"

    try {
      val inputString = fromURL(url)("UTF-8").mkString
      val inputJSON = Json.parse(inputString)

      val menusForDays = (inputJSON \ "MenusForDays").get.as[JsArray].value
      // Find the menu matching the date parameter from 'MenusForDays' array
      val maybeList = menusForDays.find(f =>
        LocalDate.parse((f \ "Date").as[String].split('T')(0)) == date)
      // If we found the list, let's continue on, otherwise return None
      if (maybeList.isDefined) {
        // defoList is definitely the list at this point. Let's get 'LunchTime' and 'SetMenus'
        val defoList = maybeList.get
        // LunchTime is the time when lunch is served / the restaurant is open
        val time = (defoList \ "LunchTime").asOpt[String]
        // SetMenus is a list of dishes and their titles like 'Pizza' > Pepperoni Pizza, Pineapple Pizza
        val setMenus = (defoList \ "SetMenus").get.as[JsArray].value

        // Convert menu items into Lines and store them in list
        val list = setMenus
          .flatMap(item => {
            // Get the name of the menu item and the actual served foods (Components)
            val name: Option[Line] =
              (item \ "Name").asOpt[String].map(f => Title(f))
            val components = (item \ "Components").as[JsArray].value

            // Now let's convert the component strings to instances of Dish and make them be in an array
            val dishes: Array[Line] = components
              .map(f => {
                val splitDish = f.as[String].split('(')
                val dishName = splitDish(0).trim
                val dishDiets = splitDish(1).dropRight(1).split(',').map(_.trim)
                Dish(dishName, Some(dishDiets))
              })
              .toArray

            // If there was a name (title) for the menu item, let's add that to the list as well
            (if (name.isDefined) dishes.+:(name.get) else dishes).toIndexedSeq
          })
          .toArray

        // If there's something in the list, let's return an instance of menu, otherwise None
        if (list.nonEmpty) Some(Menu(list, Some(time.getOrElse("Closed")))) else None
      } else None
    } catch {
      // Handle fetching from url errors
      // No internet access / API server is offline, just return no list, no need to worry, this is probably temporary.
      case _: UnknownHostException =>
        //println("No internet access / Fazer API is offline.")
        None
      // The api server is online but didn't return anything for our query
      case _: FileNotFoundException =>
        println(s"There seems to be a problem with FazerAPI when queried with $url -> The server didn't return a response.")
        None
      // Handle JSON errors (Broken API)
      // JsonParseException is an error thrown when the JSON can't be parsed and JsResultException is thrown when some property doesn't exist or it has an unexpected type
      case e @ (_: JsonProcessingException | _: JsResultException) =>
        println(s"FazerAPI provided illegal JSON response with query $url -> ${e.getMessage}")
        None
      // If there was any other kind of error with fetching the list from the api, we return nothing (None)
      case e: Exception =>
        println(s"Unknown error with FazerAPI on query $url -> ${e.getMessage}")
        None
    }
  }
}
