package feh.tec.map

import feh.tec.map.tile.{SquareTile, AbstractTile}


trait AbstractMap[Tile <: AbstractTile[Tile, Coordinate], Coordinate] {
  def tiles: Seq[Tile]

  def get: PartialFunction[Coordinate, Tile]
}

trait AbstractSquareMap[Tile <: SquareTile[Tile, (Int, Int)]] extends AbstractMap[Tile, (Int, Int)]{

  implicit class TileCoordinatesWrapper(t: Tile){
    def x = t.coordinate._1
    def y = t.coordinate._2
  }

  assertDefinedAtAllCoordinates()

  def coordinates: CoordinatesMeta
  protected def assertDefinedAtAllCoordinates()

  trait CoordinatesMeta {
    def xRange: Range
    def yRange: Range
  }
}

/**
 * Continuous, enclosed map. Is has no border and therefore every tile has exactly the same number of neighbours
 */
trait EnclosedMap[Tile <: AbstractTile[Tile, Coordinate], Coordinate] extends AbstractMap[Tile, Coordinate]{
  def nNeighbours: Int
}