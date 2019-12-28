package ui

import java.io.IOException
import java.net.URL

import javafx.scene.Parent
import scalafxml.core.FXMLLoader
import main.Lunchlist
import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
import scalafx.scene.Scene
import scalafxml.core.{FXMLView, NoDependencyResolver}

object Main extends JFXApp {

  // Initialize an instance of Lunchlist to be referenced by controllers
  val lunchList = new Lunchlist()
  lunchList.init()

  // What follows is the boilerplate for having multiple views and easily switching between them so that the code stays somewhat DRY

  // New scenes would be introduced here, assuming they are in the ui package and their file name is just the scene's name
  private val sceneNames = Array("main", "settings")

  // This function is used for initializing the scenes map
  private def sceneAndControllerFromFXMLName(name: String): (Scene, ControllerInterface) = {
    val loader = new FXMLLoader(getClass.getResource(name), NoDependencyResolver)
    loader.load()
    val root = loader.getRoot[Parent]
    // This is the controller just like mainController or the settingsController that is used for refreshing the scene's contents
    val controller = loader.getController[ControllerInterface]
    (new Scene(root), controller)
  }

  // Initialize available scenes as a map
  // This map can be used for getting the controller interface
  val scenes: Map[String, (Scene, ControllerInterface)] = sceneNames
    .map(name => (name, sceneAndControllerFromFXMLName(name + ".fxml")))
    .toMap

  // Make sure all of them actually exist to prevent errors in development...
  scenes.foreach(scene =>
      if (scene._2._1 == null)
        throw new IOException("Cannot load resource for scene " + scene._1)
  )

  // Switches to another (supported) scene and refreshes its contents
  def switchScene(sceneName: String): Unit = {
    val sceneTuple = scenes(sceneName)
    if (scenes.contains(sceneName)) {
      stage = new PrimaryStage() {
        title = "<lunchlist>"
        scene = sceneTuple._1
      }
      // Refresh the contents of the scene we're switching to.
      sceneTuple._2.refreshContents()
    } else
      throw new NoSuchElementException("The application doesn't contain a view called " + sceneName)
  }

  // Initialize the program with the main view
  switchScene("main")

  // Initialize a specific size for the stage
  stage.setHeight(755)
  stage.setWidth(1000)
}
