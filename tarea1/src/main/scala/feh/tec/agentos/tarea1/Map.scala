package feh.tec.agentos.tarea1

import feh.tec.map._
import feh.tec.map.tile.{SquareTile, OptionalMabObjectContainerTile, MapObject}
import java.util.UUID
import feh.tec.util.RangeWrapper._
import scala.{Predef, collection, Some}
import feh.tec.agent.AgentId

/*
  todo: move
 */
class Map(buildTilesMap: Map => collection.Map[(Int, Int), SqTile], xRange: Range, yRange: Range)
  extends AbstractSquareMap[SqTile] with EnclosedMap[SqTile, (Int, Int)] with AgentsPositionsProvidingMap[SqTile, (Int, Int)]
{ map =>

  type Tile = SqTile
  type Coordinate = (Int, Int)

  lazy val tilesMap = buildTilesMap(this)

  def tilesToMap = tilesMap

  def nNeighbours = 4
  def tiles: Seq[Tile] = tilesMap.values.toSeq
  def get: PartialFunction[Map#Coordinate, Tile] = tilesMap

  lazy val coordinates = new CoordinatesMeta {
    def xRange: Range = map.xRange
    def yRange: Range = map.yRange
  }


  import SimpleDirection._

  def onCoordinateGridEdge(c: (Int, Int)): Option[SimpleDirection] = c match{
    case (x, _) if x == xRange.min => Some(Left)
    case (x, _) if x == xRange.max => Some(Right)
    case (_, y) if y == yRange.min => Some(Bottom)
    case (_, y) if y == yRange.max => Some(Top)
  }

  def onCoordinateGridEdge(tile: Tile): Option[SimpleDirection] = onCoordinateGridEdge(tile.coordinate)

  protected val neighborsMap = collection.mutable.Map.empty[Tile, Seq[Tile]]

  def getNeighbors(tile: Tile) = neighborsMap get tile getOrElse neighborsMap.synchronized{
    val neighbors = findNeighbors(tile)
    neighborsMap += tile -> neighbors
    neighbors
  }

  protected def findNeighbors(tile: Tile): Seq[Tile] = {
    val edge = onCoordinateGridEdge(tile)

    Seq(
      edge.withFilter(_ == Top)     map (_ => get(tile.x, yRange.min))  getOrElse get(tile.x, tile.y + yRange.step),
      edge.withFilter(_ == Right)   map (_ => get(xRange.min, tile.y))  getOrElse get(tile.x + xRange.step, tile.y),
      edge.withFilter(_ == Bottom)  map (_ => get(tile.x, yRange.max))  getOrElse get(tile.x, tile.y - yRange.step),
      edge.withFilter(_ == Left)    map (_ => get(xRange.max, tile.y))  getOrElse get(tile.x - xRange.step, tile.y)
    )
  }

  // todo: ensure assert is always after creation executed
  protected def assertDefinedAtAllCoordinates(){
    for{
      x <- xRange
      y <- yRange
    } assert(get.isDefinedAt(x -> y), s"Map tile is not defined at ($x, $y)")
  }


  def agentsPositions: collection.Map[AgentId, Tile] = ???
}

object Map{
  class SnapshotBuilder extends MapSnapshotBuilder[Map, SqTile, (Int, Int)]{
    lazy val tilesSnapshotBuilder = new SqTile.SnapshotBuilder

    def snapshot(m: Map): MapSnapshot[Map, SqTile, (Int, Int)] = new Map(null, m.coordinates.xRange, m.coordinates.yRange) with MapSnapshot[Map, SqTile, (Int, Int)]{
      lazy val tilesSnapshots: Seq[TileSnapshot[SqTile, (Int, Int)]] = tilesSnapshotsMap.values.toSeq

      override lazy val tilesMap: collection.Map[(Int, Int), SqTile] = ???
      val tilesSnapshotsMap: collection.Map[(Int, Int), TileSnapshot[SqTile, Coordinate]] =
        m.tilesToMap.mapValues(tilesSnapshotBuilder.snapshot)

      def getSnapshot: PartialFunction[Coordinate, TileSnapshot[SqTile, Coordinate]] = tilesSnapshotsMap

      override def getNeighbors(tile: Tile): Seq[Tile] = ???
      def getNeighborsSnapshots(tile: Tile): Seq[TileSnapshot[SqTile, Coordinate]] = super.getNeighbors(tile).map(tilesSnapshotBuilder.snapshot)

      override protected def findNeighbors(tile: Tile): Seq[Tile] = ???
      def findNeighborsSnapshots(tile: Tile): Seq[TileSnapshot[SqTile, Coordinate]] = super.findNeighbors(tile).map(tilesSnapshotBuilder.snapshot)
    }
  }

  implicit class DirectionOps(map: Map){
    def tileTo(from: (Int, Int), direction: SimpleDirection): SqTile = map.get(positionTo(from, direction))

    import SimpleDirection._

    def positionTo(from: (Int, Int), direction: SimpleDirection): (Int, Int) = direction match{
      case Up if map.onCoordinateGridEdge(from).exists(_ == Top)      => from._1 -> map.coordinates.yRange.min
      case Up                                                         => from._1 -> (from._2 + 1)
      case Down if map.onCoordinateGridEdge(from).exists(_ == Bottom) => from._1 -> map.coordinates.yRange.max
      case Down                                                       => from._1 -> (from._2 - 1)
      case Left if map.onCoordinateGridEdge(from).exists(_ == Left)   => map.coordinates.xRange.max -> from._2
      case Left                                                       => (from._1 - 1) -> from._2
      case Right if map.onCoordinateGridEdge(from).exists(_ == Right) => map.coordinates.xRange.min -> from._2
      case Right                                                      => (from._1 + 1) -> from._2
    }
  }

}

case class SqTile(map: Map, coordinate: (Int, Int), contents: Option[MapObj])
  extends OptionalMabObjectContainerTile[SqTile, MapObj, (Int, Int)] with SquareTile[SqTile, (Int, Int)]
{
  def neighbours = map.getNeighbors(this)
}

object SqTile{
  class SnapshotBuilder extends TileSnapshotBuilder[SqTile, (Int, Int)]{
    def snapshot(t: SqTile): TileSnapshot[SqTile, (Int, Int)] = new SqTile(t.map, t.coordinate, t.contents) with TileSnapshot[SqTile, (Int, Int)]{
      def neighboursSnapshots: Seq[TileSnapshot[SqTile, (Int, Int)]] = super[SqTile].neighbours.map(snapshot)
    }
  }
}

trait MapObj extends MapObject{
  def isAgent: Boolean = false
  def isPlug: Boolean = false
  def isHole: Boolean = false
}
case class AgentAvatar(ag: AgentId) extends MapObj {override def isAgent = true}
case class Plug() extends MapObj { override def isPlug = true }
case class Hole(plugged: Option[Plug] = None) extends MapObj{
  override def isHole: Boolean = true

  def isPlugged = plugged.isDefined
}

object DummyMapGenerator{ outer =>
  trait DummyMapGeneratorHelpers
  trait DummyMapGeneratorHelpersBuilder[H <: DummyMapGeneratorHelpers]{
    def build(xRange: Range, yRange: Range): H
  }

  trait DummyMapGeneratorRandomPositionSelectHelper extends DummyMapGeneratorHelpers{
    def uniqueRandomPosition: (Int, Int)
  }

  implicit object DummyMapGeneratorRandomPositionSelectHelperBuilder
    extends DummyMapGeneratorHelpersBuilder[DummyMapGeneratorRandomPositionSelectHelper]
  {
    def build(xRange: Range, yRange: Range) = new DummyMapGeneratorRandomPositionSelectHelper{
      val uniqueRandomPosition: (Int, Int) = xRange.randomSelect -> yRange.randomSelect
    }
  }

  def withHelpers[H <: DummyMapGeneratorHelpers](implicit hb: DummyMapGeneratorHelpersBuilder[H]) = new WithHelpers[H]

  class WithHelpers[H <: DummyMapGeneratorHelpers](implicit hb: DummyMapGeneratorHelpersBuilder[H]) {
    def apply(xRange: Range, yRange: Range)(build: H => (Int, Int) => Option[MapObj]): Map = outer.apply(xRange, yRange)(build(hb.build(xRange, yRange)))
    def buildTilesMap(xRange: Range, yRange: Range)(build: H => (Int, Int) => Option[MapObj]) = outer.buildTilesMap(xRange, yRange)(build(hb.build(xRange, yRange)))
  }

  def apply(xRange: Range, yRange: Range)(build: (Int, Int) => Option[MapObj]): Map =
    new Map(
      xRange = xRange,
      yRange = yRange,
      buildTilesMap = buildTilesMap(xRange, yRange)(build)
    )

  def buildTilesMap(xRange: Range, yRange: Range)(build: (Int, Int) => Option[MapObj]) = (map: Map) => (
    for{
      x <- xRange
      y <- yRange
      c = x -> y
    } yield c -> SqTile(map, c, build.tupled(c))
    ).toMap
}