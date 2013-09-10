package feh.tec.agent

import scala.concurrent._
import concurrent.duration._
import feh.tec.util.{SideEffect, ScopedState}

/**
 * provides access to current environment instance, which is hidden from agent
 */
trait EnvironmentRef[Coordinate, State, Global, Action <: AbstractAction, Env <: Environment[Coordinate, State, Global, Action, Env]]{

  type EnvRef = EnvironmentRef[Coordinate, State, Global, Action, Env]

  def blocking: BlockingApi
  def async: AsyncApi

  trait BlockingApi{
    def withTimeout[R](t: Int)(r: => R): R

    def globalState: Global
    def stateOf(c: Coordinate): Option[State]
    def affected(act: Action): SideEffect[EnvRef]
    def visibleStates: Map[Coordinate, State]

    /**
     * makes a snapshot (a static image) of current environment state
     */
    def snapshot: Env with EnvironmentSnapshot[Coordinate, State, Global, Action, Env]
  }

  trait AsyncApi{
    def withTimeout[R](t: Int)(r: => R): R

    def globalState: Future[Global]
    def stateOf(c: Coordinate): Future[Option[State]]
    def affected(act: Action): Future[SideEffect[EnvRef]]
    def visibleStates: Future[Map[Coordinate, State]]

    /**
     * makes a snapshot (a static image) of current environment state
     */
    def snapshot: Future[Env with EnvironmentSnapshot[Coordinate, State, Global, Action, Env]]
  }

}

trait EnvironmentRefBlockingApiImpl[Coordinate, State, Global, Action <: AbstractAction, Env <: Environment[Coordinate, State, Global, Action, Env]]
  extends EnvironmentRef[Coordinate, State, Global, Action , Env]
{
  implicit def executionContext: ExecutionContext

  def defaultBlockingTimeout: Int
  protected val blockingTimeoutScope = new ScopedState(defaultBlockingTimeout)

  lazy val blocking = new BlockingApi{
    def withTimeout[R](t: Int)(r: => R): R = blockingTimeoutScope.doWith(t)(r)

    private def awaitResult[R](select: AsyncApi => Awaitable[R]): R = Await.result(select(async), blockingTimeoutScope.get millis)
    def globalState: Global = awaitResult(_.globalState)
    def stateOf(c: Coordinate): Option[State] = awaitResult(_.stateOf(c))
    def affected(act: Action): SideEffect[EnvRef] = awaitResult(_.affected(act))
    def visibleStates: Map[Coordinate, State] = awaitResult(_.visibleStates)
    def snapshot: Env with EnvironmentSnapshot[Coordinate, State, Global, Action, Env] = awaitResult(_.snapshot)
  }
}

/**
 * should be mixed-in last
 */
trait EnvironmentSnapshot[Coordinate, State, Global, Action <: AbstractAction, Env <: Environment[Coordinate, State, Global, Action, Env]]
  extends Environment[Coordinate, State, Global, Action, Env] /*with Determinism[Coordinate, State, Global, Action]*/{
  self: Env =>

  /**
   * @return self, no effect should be produced
   */
//  abstract override def affected(act: Action): EnvironmentSnapshot[Coordinate, State, Global, Action, Env] = this
  def affected(act: Action): SideEffect[Env] = SideEffect(this)
}
