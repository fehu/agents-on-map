package feh.tec.agentos.tarea1

import feh.tec.agentos.map.{SimpleDirection, EnclosedMap, AbstractMap, AbstractSquareMap}
import feh.tec.agentos.map.tile.OptionalTypedContainerTile
import java.util.UUID

class Map(buildTilesMap: Map => collection.Map[Map#Coordinate, SqTile], xRange: Range, yRange: Range)
  extends AbstractSquareMap with EnclosedMap
{ map =>

  type Tile = SqTile

  lazy val tilesMap = buildTilesMap(this)
  def nNeighbours = 4
  def tiles: Seq[Map#Tile] = tilesMap.values.toSeq
  def get: PartialFunction[Map#Coordinate, Tile] = tilesMap

  lazy val coordinates = new CoordinatesMeta {
    def xRange: Range = map.xRange
    def yRange: Range = map.yRange
  }


  import SimpleDirection._

  protected def onCoordinateGridEdge(tile: Tile): Option[SimpleDirection] = tile.coordinate match{
    case (x, _) if x == xRange.min => Some(Left)
    case (x, _) if x == xRange.max => Some(Right)
    case (_, y) if y == yRange.min => Some(Bottom)
    case (_, y) if y == yRange.max => Some(Top)
  }

  protected val neighborsMap = collection.mutable.Map.empty[Tile, Seq[Tile]]

  def getNeighbors(tile: Tile) = neighborsMap get tile getOrElse neighborsMap.synchronized{
    val neighbors = findNeighbors(tile)
    neighborsMap += tile -> neighbors
    neighbors
  }

  def findNeighbors(tile: Tile): Seq[Tile] = {
    val edge = onCoordinateGridEdge(tile)

    Seq(
      edge.withFilter(_ == Top)     map (_ => get(tile.x, yRange.min))  getOrElse get(tile.x, tile.y + yRange.step),
      edge.withFilter(_ == Right)   map (_ => get(xRange.min, tile.y))  getOrElse get(tile.x + xRange.step, tile.y),
      edge.withFilter(_ == Bottom)  map (_ => get(tile.x, yRange.max))  getOrElse get(tile.x, tile.y - yRange.step),
      edge.withFilter(_ == Left)    map (_ => get(xRange.max, tile.y))  getOrElse get(tile.x - xRange.step, tile.y)
    )
  }

  protected def assertDefinedAtAllCoordinates() = for{
    x <- xRange
    y <- yRange
  } assert(get.isDefinedAt(x -> y), s"Map tile is not defined at ($x, $y)")

//  def createTile(coordinate: Coordinate, contents: Option[MapObject]) = SqTile(coordinate: Coordinate, contents)
}

case class SqTile(map: Map, coordinate: Map#Coordinate, contents: Option[MapObject])
  extends OptionalTypedContainerTile[SqTile, MapObject, Map#Coordinate]
{
  def neighbours = map.getNeighbors(this)
}

trait MapObject
case class AgentAvatar(/*todo agent: Agent*/) extends MapObject
case class Plug(id: UUID) extends MapObject
case class Hole(plugged: Option[Plug]) extends MapObject{
  def isPlugged = plugged.isDefined
}

object DummyMapGenerator{
  def apply(xRange: Range, yRange: Range)(build: (Int, Int) => Option[MapObject]) =
    new Map(
      xRange = xRange,
      yRange = yRange,
      buildTilesMap = map => (
        for{
          x <- xRange
          y <- yRange
          c = x -> y
        } yield c -> SqTile(map, c, build.tupled(c))
      ).toMap
    )
}