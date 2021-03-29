package ccc

import scalafx.Includes._
import scalafx.animation.AnimationTimer
import scalafx.application.JFXApp.PrimaryStage
import scalafx.beans.property._
import scalafx.collections.ObservableBuffer
import scalafx.geometry._
import scalafx.geometry.Insets
import scalafx.geometry.Pos
import scalafx.scene.control._
import scalafx.scene.control.cell._
import scalafx.scene.paint.Color
import scalafx.scene.Scene
import scalafx.scene.layout._
import scalafx.scene.text.Text
import scalafx.util._
import zio._

case class UI(
    initialState: State,
    kafkaInterface: KafkaInterface, 
    step: State => ZIO[Env, Throwable, State]) 
    extends UIInterface {

    @Override
    def getBootstrapAddress(): ZIO[Env, Throwable, String] = 
        ZIO.succeed(bootstrapAddress())

    @Override
    def getKafkaProperties(): ZIO[Env, Throwable, Vector[(String, String)]] =
        ZIO.succeed(kafkaProperties.map({case (n,v) => (n.value, v.value)}).toVector)

    @Override
    def getAskConnect(): ZIO[Env, Throwable, Boolean] = 
        ZIO.succeed(askConnect())

    @Override
    def getSelectedTopic(): ZIO[Env, Throwable, Option[String]] =
        ZIO.succeed( selectedTopic() != null && selectedTopic().nonEmpty match {
            case false => None
            case true => Some(selectedTopic())
        }) 

    @Override
    def getSelectedPartitions(): ZIO[Env, Throwable, Vector[Int]] =
        ZIO.succeed { 
            partitions.toVector.filter(_._2()).map(_._1)
        }

    @Override
    def getIsConsuming(): ZIO[Env, Throwable, Boolean] =
        ZIO.succeed(isConsuming())

    @Override
    def getIsConnected(bool: Boolean): ZIO[Env, Throwable, Boolean] =
        ZIO.succeed(isConnected())

    @Override
    def setAlert(msg: String): ZIO[Env, Throwable, Unit] = 
        ZIO.effect {
            this.alert() = msg
        }

    @Override
    def setAskConnect(bool: Boolean): ZIO[Env, Throwable, Unit] = 
        ZIO.effect {
            this.askConnect() = bool
        }

    @Override
    def setRecords(records: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.records.clear()
            this.records.insertAll(0, records)
        }

    @Override
    def appendRecords(records: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.records.insertAll(0, records)
        }

    @Override
    def setTopics(topics: Vector[String]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.topics.clear()
            this.topics.addAll(topics)
        }

    @Override
    def setIsConnected(bool: Boolean):  ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.isConnected() = bool
        }

    @Override
    def setIsConsuming(bool: Boolean):  ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.isConnected() = bool
        }

    @Override
    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.partitions.clear()
            this.partitions.addAll( 
                partitions.map { i => (i, BooleanProperty(selectedPartitions.contains(i))) }
            )
        }

    @Override
    def setSelectedPartitions(selectedPartitions: Vector[Int]): ZIO[Env, Throwable, Unit] =
        ZIO.effect {
            this.partitions.foreach { case (partition, checked) => 
                checked() = selectedPartitions.contains(partition)
            }
        }

    def stage = new PrimaryStage {
        title = "CCC Conduktor Coding Challenge"
        scene = new Scene {
            root = new BorderPane {
                padding = Insets(25)

                top = topPanel

                center <== when(selectedTopic =!= "")
                    .choose(recordsView) 
                    .otherwise(recordsViewNoTopic)

                left <== when(isConnected)
                    .choose(topicsView) 
                    .otherwise(topicsViewNotConnected)

                right = rightPanel

                stepper.start()
            }
        }
    }

    val alert = StringProperty("")

    val bootstrapAddress = StringProperty("")

    val selectedTopic = StringProperty("")

    val kafkaProperties = Vector.tabulate(100) { (i: Int) =>
        (StringProperty(""), StringProperty(""))
    }

    val topics = ObservableBuffer[String]()

    val records = ObservableBuffer[String]()

    val partitions = ObservableBuffer[(Int, BooleanProperty)]()

    val isConnected = BooleanProperty(false)

    val isConsuming = BooleanProperty(true)

    val askConnect = BooleanProperty(false)

    val bootstrapAddressTextField = new TextField {
        focusTraversable = false
        prefColumnCount = 80
        promptText = "Enter a bootstrap address"
        text <==> bootstrapAddress
        onAction = { _ => 
            askConnect() = true
        }
    }

    val connectButton = new Button {
        text = "Connect"
        onAction = { _ => 
            askConnect() = true
        }
    }

    val env = Env(this, kafkaInterface)
    var state = initialState

    val stepper = AnimationTimer { (now: Long) => 
        state = Runtime.default.unsafeRun(step(state).provide(env))
    }

    val addressBar = new HBox {
        padding = Insets(15)
        spacing = 10
        alignment = Pos.Center
        children = Seq(bootstrapAddressTextField, connectButton)
    }

    val topicsView =  new VBox {
        padding = Insets(15)
        spacing = 10

        val vbox = this

        val header = new Text("Select a topic")
        val list = new ListView[String](topics) {
             prefHeight <== vbox.height
             selectionModel().selectedItem.onChange { (_, _, newTopic) =>
                 selectedTopic() = newTopic  
             }
         }

        children = Seq( header, list )
    }

    val topicsViewNotConnected = new VBox {
        padding = Insets(15)
        spacing = 10
        alignment = Pos.TopCenter
        children = Seq(
            new Text("Connect to a bootstrap to see topics.")
        )
    }

    val recordsView = new VBox {
        padding = Insets(15)
        spacing = 10

        val vbox = this

        val headerText = new Text {
            text <== new StringProperty("Records for topic: ").concat(selectedTopic).concat(" (most recent first)")
        }

        val consumingButton = new Button() {
            text <== when(isConsuming) choose("Stop consuming") otherwise("Start consuming")
            onAction = _ => isConsuming() = !isConsuming() 
        }

        val header = new HBox {
            padding = Insets(15)
            spacing = 10
            alignment = Pos.Center
            children = Seq( headerText, consumingButton )
        }

        val list = new ListView[String](records) {
            prefHeight <== vbox.height
            prefWidth <== vbox.width
            alignment = Pos.TopCenter
        }

        children = Seq(header, list)
    }

    val recordsViewNoTopic = new VBox {
        padding = Insets(15)
        spacing = 10
        alignment = Pos.TopCenter
        children = Seq(
            new Text("Select a topic to see records.")
        )
    }

    val kafkaPropertiesTextFields = kafkaProperties.zipWithIndex.map { 
        case ((name, value), i) => {
            val nameTextField = new TextField {
                focusTraversable = false
                prefColumnCount = 20
                promptText = "property.name." ++ i.toString()
                text <==> name
            }

            val valueTextField = new TextField {
                focusTraversable = false
                prefColumnCount = 20
                promptText = "value"
                text <==> value
            }

            (nameTextField, valueTextField)
        }
    }

    val kafkaPropertiesEditor = new VBox {
        padding = Insets(15)
        spacing = 10

        val header = new Text("Kafka properties")

        val table = new ScrollPane {
            prefHeight = 500
            content = new GridPane {
                hgap = 10
                vgap = 10
            
                kafkaPropertiesTextFields.zipWithIndex.foreach { 
                    case ((name, value), i) => {
                        this.add(name, 0, i)
                        this.add(value, 1, i)
                    }
                }
            }
        }

        children = Seq(header, table)
    }

    val partitionSelector = new VBox {
        padding = Insets(15)
        spacing = 10

        val header = new Text("Consume from partitions")

        val list = new ListView[(Int, BooleanProperty)](partitions) {
            prefHeight = 500
            cellFactory = CheckBoxListCell.forListView[(Int, BooleanProperty)]( 
                selectedProperty = { 
                    //TODO l'appariement bidirectionnel ne fonctionne pas
                    (pair: (Int, BooleanProperty)) => pair._2
                },
                converter = StringConverter.toStringConverter { 
                    (pair: (Int, BooleanProperty)) => pair._1.toString()
                })
            //selectionModel().setSelectionMode(SelectionMode.MULTIPLE);
         }

        children = Seq(header, list)
    }

    val topPanel = new VBox {
        padding = Insets(15)
        spacing = 10
        alignment = Pos.Center

        val alertMsg = new Text {
            text <== alert
            fill = Color.Red
        }

        children = Seq( alertMsg, addressBar )
    }

    val rightPanel = new VBox {
        padding = Insets(15)
        spacing = 10

        children = Seq( partitionSelector, kafkaPropertiesEditor )
    }

}
