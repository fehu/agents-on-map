package feh.tec.agentos.tarea1

import feh.tec.map._
import feh.tec.agent._
import scala.reflect.runtime.universe._
import akka.actor.{ActorSystem, Scheduler}
import scala.concurrent.ExecutionContext

object Environment{
  type Tile = SqTile
  type Coordinate = Map#Coordinate
  type State = MapState[Coordinate, Tile, Map]
  type Global = MapGlobalState[Coordinate, Tile, Map]
  type Action = MapAction[Coordinate, Tile, Map]
}

import Environment._

class Environment(buildTilesMap: Map => collection.Map[(Int, Int), SqTile],
                  val xRange: Range,
                  val yRange: Range,
                  val effects: PartialFunction[Action, Environment => Environment],
                  val initStates: PartialFunction[Coordinate, State],
                  val initGlobalState: Global)
  extends Map(buildTilesMap, xRange, yRange)
  with AbstractMapEnvironment[Map, Tile, Coordinate, State, Global, Action, Environment]
  with FullyAccessible[Coordinate, State, Global, Action, Environment]
  with Deterministic[Coordinate, State, Global, Action, Environment]
  with Static[Coordinate, State, Global, Action, Environment]
  with MutableEnvironment[Coordinate, State, Global, Action, Environment]
{

  lazy val definedAt: Seq[Coordinate] = xRange zip yRange

  lazy val tags = new TypeTags{
    implicit def coordinate: TypeTag[Coordinate] = typeTag[Coordinate]
    implicit def state: TypeTag[State] = typeTag[State]
    implicit def global: TypeTag[Global] = typeTag[Global]
    implicit def action: TypeTag[Action] = typeTag[Action]
    implicit def environment: TypeTag[Environment] = typeTag[Environment]
  }
}

class Overseer(actorSystem: ActorSystem,
               val defaultBlockingTimeout: Int,
               val defaultFutureTimeout: Int,
               initEnvironment: Environment)
  extends EnvironmentOverseerActor[Coordinate, State, Global, Action, Environment]
  with MutableEnvironmentOverseer[Coordinate, State, Global, Action, Environment]
{
  
  overseer =>
  
  protected def refExecutionContext: ExecutionContext = actorSystem.dispatcher
  protected def scheduler: Scheduler = actorSystem.scheduler

  override val currentEnvironment: Environment = initEnvironment


  def snapshot: EnvironmentSnapshot[Coordinate, State, Global, Action, Environment] =
    new Environment(null, env.xRange, env.yRange, env.effects, env.initStates, env.initGlobalState)
      with EnvironmentSnapshot[Coordinate, State, Global, Action, Environment]
    {
      override val states = initStates
      override def states_=(pf: PartialFunction[Environment.Coordinate, Environment.State]) {}
      override val globalState = initGlobalState
      override def globalState_=(g: Environment.Global) {}
      override lazy val tilesMap = env.tilesMap

      /**
       * @return self, no effect should be produced
       */
      override def affected(act: Environment.Action) = super[EnvironmentSnapshot].affected(act)
    }
}