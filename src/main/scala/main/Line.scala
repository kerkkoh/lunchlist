package main

sealed trait Line {
  val name: String
  val diets: Option[Array[String]]
  def canEqual(a: Any): Boolean
  override def equals(that: Any): Boolean =
    that match {
      case that: Line =>
        that.canEqual(this) && this.name == that.name && this.diets
          .getOrElse(Array.empty[String])
          .deep == that.diets.getOrElse(Array.empty[String]).deep
      case _ => false
    }
}

case class Title(name: String, diets: Option[Array[String]] = None) extends Line {
  override def canEqual(a: Any): Boolean = a.isInstanceOf[Title]
}
case class Dish(name: String, diets: Option[Array[String]] = None) extends Line {
  override def canEqual(a: Any): Boolean = a.isInstanceOf[Dish]
}
