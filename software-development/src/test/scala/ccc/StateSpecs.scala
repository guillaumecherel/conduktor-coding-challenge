import org.scalatest._
import org.scalatest.Inside._
import flatspec._
import matchers._

import zio._

import ccc._

class StateSpec extends AnyFlatSpec with should.Matchers {

    val runtime = Runtime.default

    "The state" should "connect to a bootstrap when asked." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true

        val ka = KafkaTest()

        val env = Env(ui, ka)

        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .provide(env)
        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(bootstrapAddress, _, _, _) =>
                bootstrapAddress should be ("abcd")
            }
        }
        
    }

    it should "get a list of topics when connecting to a consumer." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true

        val ka = KafkaTest()

        val env = Env(ui, ka)

        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .provide(env)

        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(_, _, topicNames, _) =>
                topicNames should be (Vector("topic1", "topic2", "topic3"))
            }
        }
        
    }

    it should "poll records from the beginning." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true
        ui.selectedTopic = Some("topic1")
        ui.isConsuming = true

        val ka = KafkaTest()

        val env = Env(ui, ka)
      
        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .provide(env)
        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(_, _, _, topic) =>
                inside(topic) { case t : OpenedTopic => 
                    t.buffer should be (Vector("a", "b", "c")) 
                }
            }
        }
    }

    it should "poll additionnal records." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true
        ui.selectedTopic = Some("topic1")
        ui.isConsuming = true

        val ka = KafkaTest()

        val env = Env(ui, ka)
      
        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
             .provide(env)
        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(_, _, _, topic) =>
                inside(topic) { case t : OpenedTopic => 
                    t.buffer should be (Vector("a", "b", "c", "a", "b", "c"))
                }
            }
        }
    }

    it should "not poll records when not consuming." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true
        ui.selectedTopic = Some("topic1")
        ui.isConsuming = false

        val ka = KafkaTest()

        val env = Env(ui, ka)
      
        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
             .provide(env)
        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(_, _, _, topic) =>
                inside(topic) { case t : OpenedTopic => 
                    t.buffer should be (Vector.empty)
                }
            }
        }
    }

    it should "poll records from the selected partitions only." in {
        val ui = UITest()

        val ka = KafkaTest()

        val env = Env(ui, ka)
      
        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(s => ZIO.effect {
                ui.bootstrapAddress = "abcd"
                ui.askConnect = true
                s
            })
            .flatMap(State.update)
            // InitiatingConnection
            .flatMap(State.update)
            // Connecting
            .flatMap(State.update)
            // Connected, NoTopic
            .flatMap(s => ZIO.effect {
                ui.selectedTopic = Some("topic1")
                ui.isConsuming = true
                s
            })
            .flatMap(State.update)
            // Connected, InitiatingOpeningTopic
            .flatMap(State.update)
            // Connected, OpeningTopic
            .flatMap(State.update)
            // Connected, OpenedTopic
            .flatMap(s => ZIO.effect {
                ui.selectedPartitions = Vector(1)
                s
            })
            .flatMap(State.update)
            // Connected, OpenedTopic
            .flatMap(State.update)
            .flatMap(State.update)
            .provide(env)
        } 

        withClue("State: " ++ state.toString() ++ "\n") {
            inside(state) { case Connected(_, _, _, topic) =>
                inside(topic) { case t : OpenedTopic => 
                    t.buffer should be (Vector("b", "b"))
                }
            }
        }
    }

    it should "update the UI with its records." in {
        val ui = UITest()
        ui.bootstrapAddress = "abcd"
        ui.askConnect = true
        ui.selectedTopic = Some("topic1")
        ui.isConsuming = true

        val ka = KafkaTest()

        val env = Env(ui, ka)
      
        val state = runtime.unsafeRun {
            ZIO.succeed(Disconnected())
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
            .flatMap(State.update)
             .provide(env)
        } 

        ui.records should be (Vector("a", "b", "c", "a", "b", "c"))
    }
}

