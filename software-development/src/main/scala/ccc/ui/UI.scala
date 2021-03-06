package ccc.ui

import javafx.application.Platform
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

import ccc.Env
import ccc.errors._
import ccc.state.State
import ccc.kafka.KafkaInterface

/**
  * The scalafx user interface. Since Scalafx controls the main loop, we pass
  * to it the step function which is responsible for updating the main
  * application state. It also takes care of creating the application
  * environment Env consisting of the given KafkaInterface and this UI itself.
  * The overriden effectful method mutate the UI state.
  *
  * @param initialState
  * @param kafkaInterface
  * @param step
  */
case class UI(
    initialState: State,
    kafkaInterface: KafkaInterface, 
    step: State => ZIO[Env, Throwable, State])
    extends UIInterface {

    def getBootstrapAddress(): ZIO[Any, Nothing, String] = 
        ZIO.effectTotal(bootstrapAddress())

    def getKafkaProperties(): ZIO[Any, Nothing, Vector[(String, String)]] =
        ZIO.effectTotal(kafkaProperties.map({case (n,v) => (n.value, v.value)}).toVector)

    def getAskConnect(): ZIO[Any, Nothing, Boolean] = 
        ZIO.effectTotal(askConnect())

    def getSelectedTopic(): ZIO[Any, Nothing, Option[String]] =
        ZIO.effectTotal( selectedTopic() != null && selectedTopic().nonEmpty match {
            case false => None
            case true => Some(selectedTopic())
        }) 

    def getSelectedPartitions(): ZIO[Any, Nothing, Vector[Int]] =
        ZIO.effectTotal { 
            partitions.toVector.filter(_._2()).map(_._1)
        }

    def getIsConsuming(): ZIO[Any, Nothing, Boolean] =
        ZIO.effectTotal(isConsuming())

    def getIsConnected(bool: Boolean): ZIO[Any, Nothing, Boolean] =
        ZIO.effectTotal(isConnected())

    def setAlert(msg: String): ZIO[Any, Nothing, Unit] = 
        ZIO.effectTotal {
            this.alert() = msg
            this.alertColor() = Color.Red
        }

    def setInfo(msg: String): ZIO[Any, Nothing, Unit] = 
        ZIO.effectTotal {
            this.alert() = msg
            this.alertColor() = Color.Black
        }

    def setAskConnect(bool: Boolean): ZIO[Any, Nothing, Unit] = 
        ZIO.effectTotal {
            this.askConnect() = bool
        }

    def setRecords(records: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.records.clear()
            this.records.insertAll(0, records.reverseIterator)
        }

    def appendRecords(records: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.records.insertAll(0, records.reverseIterator)
        }

    def setTopics(topics: Vector[String]): ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.topics.clear()
            this.topics.addAll(topics)
        }

    def setIsConnected(bool: Boolean):  ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.isConnected() = bool
        }

    def setIsConsuming(bool: Boolean):  ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.isConnected() = bool
        }

    def setPartitions(partitions: Vector[Int], selectedPartitions: Vector[Int]): 
        ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
            this.partitions.clear()
            this.partitions.addAll( 
                partitions.sorted.map { i => 
                    (i, BooleanProperty(selectedPartitions.contains(i))) 
                }
            )
        }

    def setSelectedPartitions(selectedPartitions: Vector[Int]): 
        ZIO[Any, Nothing, Unit] =
        ZIO.effectTotal {
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

                // Start the main application state update loop.
                stepper.start()
            }
        }
    }

    val alert = StringProperty("Please enter a bootstrap address and port like localhost:9092")

    val alertColor = ObjectProperty(Color.Black)

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
        prefColumnCount = 60
        promptText = "Enter a bootstrap address and port (ex. localhost:9092)"
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

    // This AnimationTimer is responsible for continuously updating the 
    // application main state.
    val stepper = AnimationTimer { (now: Long) => 
        try {
            state = Runtime.default.unsafeRun(step(state).provide(env))
        } catch { 
            case (e: Throwable) => {
                // This will call ccc.App.stopApp, properly closing the 
                // environment (thus any kafka resource attached)
                Platform.exit()
                throw e
            }
        }
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

        val header = new FlowPane {
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
                promptText = "property.name." ++ i.toString()
                text <==> name
            }

            val valueTextField = new TextField {
                focusTraversable = false
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
                    (pair: (Int, BooleanProperty)) => pair._2
                },
                converter = StringConverter.toStringConverter { 
                    (pair: (Int, BooleanProperty)) => pair._1.toString()
                })
         }

        children = Seq(header, list)
    }

    val topPanel = new VBox {
        padding = Insets(15)
        spacing = 10
        alignment = Pos.Center

        val vbox = this

        val alertMsg = new Text {
            text <== alert
            fill <== alertColor
        }

        children = Seq( alertMsg, addressBar )
    }

    val rightPanel = new VBox {
        padding = Insets(15)
        spacing = 10

        children = Seq( partitionSelector, kafkaPropertiesEditor )
    }

}
