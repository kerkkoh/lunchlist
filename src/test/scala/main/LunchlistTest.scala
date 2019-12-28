package main

import java.io.FileNotFoundException

import com.fasterxml.jackson.core.JsonProcessingException
import org.scalatest._
import play.api.libs.json.{JsResultException, Json}

import scala.io.Source.fromFile
import scala.reflect.io.File

class LunchlistTest extends FlatSpec {
  "Lunchlist" should "populate restaurants array with default restaurants when there isn't a restaurants.json file" in {
    File("json/restaurants.json").delete()
    val ll = new Lunchlist()
    assert(ll.restaurants.length>0, "variable restaurants in Lunchlist wasn't populated with any default data")
  }
  "Lunchlist and Settings" should "save and load settings correctly" in {
    // First save old settings so that the test won't have side effects
    val oldSettings = new Settings()
    oldSettings.load()

    val lunchlist = new Lunchlist(new Settings(Array("*"), "SomethingSomething", Array("something")))
    assert(lunchlist.settings.favRestaurantName=="SomethingSomething", "Favorite restaurant name should have been SomethingSomething, was "+lunchlist.settings.favRestaurantName)
    assert(lunchlist.settings.diets.contains("*"), s"Diets should have contained *, instead it was ${lunchlist.settings.diets}")
    assert(lunchlist.settings.highlighted.contains("something"), s"Highlighted words should have contained 'something', but instead it was ${lunchlist.settings.highlighted}")
    assert(lunchlist.settings.favRestaurantName=="SomethingSomething", "Favorite restaurant name should have been SomethingSomething, was "+lunchlist.settings.favRestaurantName)
    // Saves the favorite restaurant name
    lunchlist.settings.save()
    // Spins up a new instance with different settings
    val lunchlist2 = new Lunchlist(new Settings(Array("L"), "SomethingElse", Array("else")))
    // Should load up the settings saved in the first instance
    lunchlist2.settings.load()
    assert(lunchlist2.settings.favRestaurantName=="SomethingSomething")
    assert(lunchlist2.settings.diets.contains("*"))
    assert(lunchlist2.settings.highlighted.contains("something"))
    // Save settings again, just to be sure
    lunchlist2.settings.save()
    try {
      val jsonData = Json.parse( fromFile("json/settings.json").mkString )
      val favRestaurantName = (jsonData \ "fav").get.asOpt[String].getOrElse("")
      assert(favRestaurantName=="SomethingSomething")
    } catch {
      // Settings file doesn't exist, let's make one
      case _: FileNotFoundException =>
        fail("After saving the settings, couldn't find a file in 'json/settings.json'")
      // Faulty JSON format (couldn't parse or couldn't get some required property)
      case e @ (_ : JsonProcessingException | _ : JsResultException) =>
        fail(s"Faulty JSON format in json/settings.json. ${e.getMessage}")
      // Print other errors
      case e: Exception =>
        fail()
        e.printStackTrace()
    }
    // Save the old settings back in to avoid side effects from the test
    oldSettings.save()
  }
}
