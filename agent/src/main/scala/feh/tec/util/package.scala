package feh.tec

import scala.collection.TraversableLike
import scala.concurrent.duration._
import java.util.Calendar

package object util {
  implicit class PipeWrapper[T](t: => T){
    def pipe[R](f: T => R): R = f(t)
    def |>[R](f: T => R): R = f(t)
  }

  implicit class LiftWrapper[T](t: =>T){
    def lift = () => t
    def lifted = lift
    def liftUnit = () => t: Unit
  }

  implicit class FilteringHelpingWrapper[+A, +Repr](tr: TraversableLike[A, Repr]){
    private def filter[B](f: A => B, v: B) = tr.filter(e => f(e) == v)

    def filterMax[B](f: A => B)(implicit cmp: Ordering[B]): Repr = filter(f, tr.maxBy(f) |> f)

    def filterMin[B](f: A => B)(implicit cmp: Ordering[B]): Repr = filter(f, tr.minBy(f) |> f)
  }

  def elapsed[R](f: => R): (R, Duration) = {
    val time1 = Calendar.getInstance().getTimeInMillis
    val res = f
    val time2 = Calendar.getInstance().getTimeInMillis
    val dur = Duration(time2 - time1, MILLISECONDS)
    res -> dur
  }

  def tryo[R](f: => R): Either[R, Throwable] = try Left(f) catch { case th: Throwable => Right(th) }
}
