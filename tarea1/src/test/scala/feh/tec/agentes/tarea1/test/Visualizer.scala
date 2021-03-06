package feh.tec.agentes.tarea1.test

import feh.tec.visual.api.{BasicStringDrawOps, BasicSquareMapDrawOptions, Easel}
import feh.tec.visual.NicolLike2DEasel
import feh.tec.agentes.tarea1.{NicolBasedTarea1AgentApp, MapJsonSerializer, Map}
import nicol._
import nicol.input.Key._
import nicol.Init
import scala.io.Source
import spray.json._
import feh.tec.visual.api.StringAlignment.Center
import java.awt.Color

trait Visualizer[E <: Easel, V] {
  def easel: Easel
  def visualisation: V

  def show()
  def close()

}

class NicolLikeMapsVisualizer(val maps: Seq[Map])
  extends Visualizer[NicolLike2DEasel, Map]
{
  implicit val easel = new NicolLike2DEasel
  val howToDraw: NicolLike2DEasel#MDrawOptions = BasicSquareMapDrawOptions(50, showLabels = true, Color.yellow)

  protected var _visualisation: Map = maps.head
  def visualisation: Map = _visualisation

  val n = maps.length
  protected var currentMap: Int = 0
  def nextMap = if(currentMap == n - 1) None else {
    currentMap += 1
    Some(maps(currentMap))
  }
  def previousMap = if(currentMap == 0) None else {
    currentMap -= 1
    Some(maps(currentMap))
  }

  def setVisualization(map: Map) = _visualisation = map

  val tileSize = 50
  val (sizeX, sizeY) = maps.head.coordinates.xRange.size*tileSize -> maps.head.coordinates.yRange.size*tileSize

  val mapRenderer = NicolBasedTarea1AgentApp.mapRenderer()
  val nicolGame = new Game(Init("Tests", sizeX + 100, sizeY + 150, vSync = false) >> new LoopScene with SyncableScene{
    def update: Option[Scene] = {
      sync

      renderMap()

      def processListing(map: Option[Map]) = {
        map.map(setVisualization).map(_ => renderMap())
        this
      }

      easel.drawString(s"[${currentMap+1}/$n]", 30F -> 15F, BasicStringDrawOps[NicolLike2DEasel](Center, Color.yellow, "arial", 20F, 5))

      keyEvent {
        e =>
          e released {
            case _ =>
          }
          e pressed {
            case "escape" => End(sys.exit())
            case "left" =>
              processListing(previousMap)
            case "right" =>
              processListing(nextMap)
          }
      }
    }
  }){}

  protected def renderMap() = mapRenderer.render(visualisation, howToDraw)

  def show() { nicolGame.start }
  def close() { nicolGame.stop }
}

object Visualize extends App{
  val serializer = new MapJsonSerializer

  val sources = args.map(Source.fromFile)
  val maps = sources.map(source => serializer.world(source.getLines().mkString("\n").asJson))
  val vis = new NicolLikeMapsVisualizer(maps)
  vis.show()
}