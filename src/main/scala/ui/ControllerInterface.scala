package ui

// An interface to use when refreshing controllers of other scenes from other
// Right now only used for when settings have been changed and the main scene needs to refresh as well.
trait ControllerInterface {
  def refreshContents(): Unit
}
