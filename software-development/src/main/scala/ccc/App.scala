package ccc

import zio._
import zio.console._

import scalafx.application.JFXApp
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property._ 
import scalafx.collections.ObservableBuffer
import scalafx.geometry.Insets
import scalafx.scene.control._
import scalafx.scene.control.cell.TextFieldTableCell
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.util.StringConverter
import scalafx.Includes._
import scalafx.geometry.Pos

object App extends JFXApp {

    val kafkaInterface = KafkaService
    val ui = UI(Disconnected(), kafkaInterface, State.step)

    stage = ui.stage
}
