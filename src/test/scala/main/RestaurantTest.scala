package main

import java.io.FileNotFoundException
import java.time.LocalDate

import com.fasterxml.jackson.core.JsonProcessingException
import org.scalatest.FlatSpec
import play.api.libs.json.{JsResultException, Json}

import scala.io.Source.fromFile
import scala.reflect.io.File

// Tester API used for testing the class
object TestAPI extends ListAPI {
  // Kill-switch that kills the api so it doesn't give any data
  var killSwitch = false
  // Dummy data
  val dummyAPI = Map(
    "1234" -> Menu(
      Array(
        Title("Dummy"),
        Dish("ABC"),
        Dish("BBC", Some(Array("L", "G"))),
        Dish("CBC", Some(Array("L"))) ),
      Some("01.00-02.00")
    )
  )
  override def fetch(date: LocalDate, apiKey: String): Option[Menu] = {
    if (killSwitch || !dummyAPI.isDefinedAt(apiKey)) None else Some(dummyAPI(apiKey))
  }
}

class RestaurantTest extends FlatSpec {

  behavior of "Test Restaurant"

  // Delete an old testaurant list file if exists
  File("json/testaurant-savedLists.json").delete()

  val r = new Restaurant("Testaurant", TestAPI, "1234")
  val api = TestAPI

  it should "return correct name when initialized with one" in {
    assert(r.name == "Testaurant")
  }

  it should "get a correct name, list and opening time from a dummy API" in {
    val list = r.getList(LocalDate.now, new Settings())
    assert(list._1=="Testaurant")
    assert(list._2.body.deep==api.dummyAPI("1234").body.deep)
    assert(list._2.lunchTime.getOrElse("Closed")==api.dummyAPI("1234").lunchTime.get)
  }

  it should "save the restaurant's list in memory" in {
    // turn on the API kill-switch
    api.killSwitch = true
    // get list
    val list = r.getList(LocalDate.now, new Settings())
    // should be the same list as the one from the api
    assert(list._2.body.deep==api.dummyAPI("1234").body.deep)
    // turn off the API kill-switch
    api.killSwitch = false
  }

  it should "create a new json file for restaurant's lists in 'json' folder with some content" in {
    try {
      val data = fromFile("json/testaurant-savedLists.json").mkString
      assert(data.length > 0, "Data in 'json/testaurant-savedLists.json' was not longer than 0 characters")
    } catch {
      // file doesn't exist
      case _: FileNotFoundException =>
        fail("Couldn't find the file 'json/testaurant-savedLists.json'")
      // Print other errors
      case e: Exception =>
        fail(e.getMessage)
        e.printStackTrace()
    }
  }

  it should "create a new json file for restaurant's lists in 'json' folder with proper syntax" in {
    try {
      val jsonData = Json.parse( fromFile("json/testaurant-savedLists.json").mkString )
      // Was able to parse, hence the test was passed if we ever got here
      assert(true)
    } catch {
      case _: FileNotFoundException =>
        fail("Couldn't find the file 'json/testaurant-savedLists.json'")
      // Faulty JSON format
      case e @ (_ : JsonProcessingException | _ : JsResultException) =>
        fail(s"Faulty JSON format in 'json/testaurant-savedLists.json' -> ${e.getMessage}")
      // Print other errors
      case e: Exception =>
        fail(e.getMessage)
        e.printStackTrace()
    }
  }

  it should "parse the dummy list properly according to the provided settings" in {
    var list = r.getList(LocalDate.now, new Settings(Array("L")))
    var listOfDishes = list._2.body.filter(line => line.getClass.getSimpleName=="Dish")
    assert(listOfDishes.forall(line =>
      line.diets.get.contains("L")
    ), "When parsing with diets ['L'], list contained Dishes that didn't have diet L")

    list = r.getList(LocalDate.now, new Settings(Array("L", "G")))
    listOfDishes = list._2.body.filter(line => line.getClass.getSimpleName=="Dish")
    assert(list._2.body.filter(line => line.getClass.getSimpleName=="Dish").forall(line =>
      line.diets.get.contains("L") && line.diets.get.contains("G")
    ), "When parsing with diets ['L', 'G'], list contained Dishes that didn't have diet L or G")
  }

}
