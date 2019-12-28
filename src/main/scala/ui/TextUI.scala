/*
!! DEPRECATED !!
!! DEPRECATED !!
!! DEPRECATED !!
!! DO NOT USE !!

package ui

import java.time.{DayOfWeek, LocalDate}
import java.util.Locale

import main._

import scala.io.StdIn
import scala.collection.breakOut
import util.control.Breaks._

object TextUI extends App {
  // Create a new Lunchlist with default values
  val lunchList = new Lunchlist()
  lunchList.init()

  var curShownDate = LocalDate.now()

  private def line(): Unit = println("------------")
  private def div(contents: String*): Unit = {
    line()
    contents.foreach(input => println(input))
    line()
  }

  val days = (DayOfWeek.values().map(_.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)) zip DayOfWeek.values())(breakOut): Map[String,DayOfWeek]

  private def getDays(): Array[String] = {
    val allDays = DayOfWeek.values()
    val today = LocalDate.now().getDayOfWeek
    allDays.dropWhile(p => p.getValue < today.getValue).map(_.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH))
  }

  private def printList(): Unit = {
    println("<lunchlist>")
    div("Commands: day, settings, quit","Format for day: day shortWeekday | Ex. day Mon")
    println("Days: " + getDays.mkString(", "))
    div(curShownDate.getDayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)+" "+curShownDate.getDayOfMonth+"."+curShownDate.getMonthValue+".")
    val lists = lunchList.getLists(curShownDate)
    lists.find(_._1==lunchList.settings.favRestaurantName).foreach(favlist => {
      println("<3 "+favlist._1+" ("+favlist._3+")")
      div(favlist._2.trim)
    })
    lists.foreach(list => {
      if (lunchList.settings.favRestaurantName != list._1) {
        println(""+list._1+" ("+list._3+")")
        div(list._2.trim)
      }
    })
  }
  private def printSettings(): Unit = {
    println("<settings>")
    div("All available diets: "+lunchList.diets.keys.mkString(", "), "Commands: hl+, hl-, fav, diet+, diet-, back")
    val settings = lunchList.settings
    println("Highlighted: (ex. hl+ thing / hl- thing)")
    settings.highlighted.foreach(f => println("- "+f))
    println("Favorite restaurant: (ex. fav name)")
    println(settings.favRestaurantName)
    println("Special diets: (ex. diet+ abbreviation / diet- abbreviation)")
    println(settings.diets.mkString(", "))
  }
  breakable {
    while(true) {
      printList()
      val input = StdIn.readLine().split(' ')

      input(0) match {
        case "quit" => break
        case "day" => {
          if (days.isDefinedAt(input(1))) {
            val weekd = days(input(1))
            val weekdNbr = weekd.getValue
            val todayNbr = LocalDate.now.getDayOfWeek.getValue
            if (weekdNbr>=todayNbr) {
              val diff = weekdNbr-todayNbr
              curShownDate = LocalDate.now.plusDays(diff)
            } else println("You can only view lunchlists for future dates or the current day..")
          } else println("Unknown day.")
        }
        case "settings" => {
          breakable {
            while(true) {
              printSettings()
              val ipt = StdIn.readLine().split(' ')
              val settings = lunchList.settings
              if (ipt.length == 0 || (ipt.length == 1 && ipt(0) != "back")) {
                println("Unknown command / invalid params.")
              } else {
                ipt(0) match {
                  case "hl+" => settings.highlighted = settings.highlighted :+ ipt(1)
                  case "hl-" => settings.highlighted = settings.highlighted.filter(_ != ipt(1))
                  case "fav" =>
                    if (lunchList.restaurants.map(_.name).contains(ipt(1)))
                      settings.favRestaurantName = ipt(1)
                    else
                      println("Unknown restaurant.")
                  case "diet+" =>
                    if (lunchList.diets.contains(ipt(1)))
                      settings.diets = settings.diets :+ ipt(1)
                    else
                      println("Unknown allergen.")
                  case "diet-" =>
                    if (lunchList.diets.contains(ipt(1)))
                      settings.diets = settings.diets.filter(_ != ipt(1))
                    else
                      println("Unknown allergen.")
                  case "back" =>
                    lunchList.settings.save()
                    break
                  case _ => println("Unknown command.")
                }
              }
            }
          }
        }
        case _ => println("Unknown command.")
      }
    }
  }

}
*/