package main

import java.time.LocalDate

abstract class ListAPI {
  def fetch(date: LocalDate = LocalDate.now(), apiKey: String): Option[Menu]
}
