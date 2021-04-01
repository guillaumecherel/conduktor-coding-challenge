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

import ccc.kafka.KafkaService
import ccc.state.Disconnected
import ccc.state.State
import ccc.ui.UI

object App extends JFXApp {

    val ui = UI(Disconnected(), KafkaService, State.step)

    stage = ui.stage
}
