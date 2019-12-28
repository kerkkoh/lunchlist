package main

import java.time.LocalDate

import play.api.libs.json.{JsArray, Json, JsResultException}
import com.fasterxml.jackson.core.JsonProcessingException
import java.io.FileNotFoundException
import java.net.UnknownHostException

import scala.io.Source.fromURL

object SodexoAPI extends ListAPI {
  override def fetch(date: LocalDate = LocalDate.now(), apiKey: String): Option[Menu] = {
    // Pads an int to two digits and to string
    def padToTwo(i: Int): String = i.toString.reverse.padTo(2, '0').reverse

    val year = date.getYear
    val month = padToTwo(date.getMonthValue)
    val day = padToTwo(date.getDayOfMonth)

    val url = s"https://www.sodexo.fi/ruokalistat/output/daily_json/$apiKey/$year/$month/$day/en"

    try {
      // Fetch data from the url
      val inputString = fromURL(url)("UTF-8").mkString

      // Parse the fetched data
      val inputJSON = Json.parse(inputString)

      // Get all courses (dishes) of the day
      val courses = (inputJSON \ "courses").get.as[JsArray].value

      if (courses.nonEmpty) {
        // Go through courses and convert them to Dish instances and convert the sequence to array
        val list = courses.map(course => {
          // Get the name of the course and replace every '&amp;' with its character &
          val name = (course \ "title_en").as[String].replace("&amp;", "&")

          // Get the diets it suits if there are any and change string to an array representation
          val diets = (course \ "properties")
            .asOpt[String]
            .map(f => f.split(',').map(_.trim))
          // Create the actual dish instances with special diets as an array rather than a string
          Dish(name, diets)
        })
        Some(Menu(list.toArray, None))
      } else None
    } catch {
      // Handle fetching from url errors
      // No internet access / API server is offline, just return no list, no need to worry, this is probably temporary.
      case _: UnknownHostException =>
        //println("No internet access / Sodexo API is offline.")
        None
      // The api server is online but didn't return anything for our query
      case _: FileNotFoundException =>
        println(s"There seems to be a problem with SodexoAPI when queried with $url -> The server didn't return a response.")
        None
      // Handle JSON errors (Broken API)
      // JsonParseException is an error thrown when the JSON can't be parsed and JsResultException is thrown when some property doesn't exist or it has an unexpected type
      case e @ (_: JsonProcessingException | _: JsResultException) =>
        println(s"SodexoAPI provided illegal JSON response with query $url -> ${e.getMessage}")
        None
      // If there was any other kind of error with fetching the list from the api, we return nothing (None)
      case e: Exception =>
        println(s"Problem with SodexoAPI on query $url -> ${e.getMessage}")
        None
    }
  }
}
