package feh.tec.agentes.tarea1

import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import feh.tec.util.FileUtils.ByteArrayToFileWrapper
import java.io.File

class EnvironmentSpec extends Specification with ScalaCheck with Arbitraries{
  import Conf._

  createTestsFolder()

  "The Environment" should{
    "be accessible at all coordinates defined" in prop{ env: Environment => env.definedAt forall env.get.isDefinedAt }
    "contain agent's avatar" in prop{ref: Environment#Ref => ref.blocking.agentPosition(agentId) must beSome}
    "provide sense information correctly" in prop{
      (overseer: Overseer) =>
        val ref = overseer.ref
        val env = overseer.env

        "about visible coordinates" >> { ref.blocking.visibleStates.keySet mustEqual env.definedAt.toSet } &&
        "about states at given coordinates" >> {
          val bulked = ref.blocking.visibleStates
          val resSeq = for(c <- bulked.keys; state = env.stateByAtom(env.atomsMap(c))) yield
            ref.blocking.stateOf(c) must beSome(state) and bulked(c).mustEqual(state)
          resSeq.all
        }

    }
    "respond to actions: " in prop {
      (overseer: Overseer) =>
        val ref = overseer.ref
        val env = overseer.env

        def pos = ref.blocking.agentPosition(agentId).get
        def passOpt(pos: (Int, Int)) = if(ref.blocking.stateOf(pos).exists(_.hole)) None else Some(pos)
        val iPos = pos
        val northPos = passOpt(iPos._1 -> (if(iPos._2  == env.coordinates.yRange.min) env.coordinates.yRange.max else iPos._2 - 1)) getOrElse iPos
        val eastPos = passOpt((if(northPos._1 == env.coordinates.xRange.max) env.coordinates.xRange.min else northPos._1 + 1) -> northPos._2) getOrElse northPos
        val southPos = passOpt(eastPos._1 -> (if(eastPos._2 == env.coordinates.yRange.max) env.coordinates.yRange.min else eastPos._2 + 1)) getOrElse eastPos
        val westPos = passOpt((if(southPos._1 == env.coordinates.xRange.min) env.coordinates.xRange.max else southPos._1 - 1) -> southPos._2) getOrElse southPos

        lazy val serializer = new MapJsonSerializer

        val pref = "tests" + File.separator

        s"positions: init=$iPos, north=$northPos, east=$eastPos, south=$southPos, west=$westPos".getBytes.toFile(pref + "positions")
        env.atoms.toSeq.mkString("\n").getBytes.toFile(pref + "init-map")

        def screenshot(file: String) = serializer.serialize(env).prettyPrint.getBytes.toFile(file)

        sequential
        screenshot(pref + "0-init")
        "move north" >> { ref.blocking.affect(MoveNorth); screenshot(pref + "1-north"); pos mustEqual northPos } &&
        "move east"  >> { ref.blocking.affect(MoveEast);  screenshot(pref + "2-east");  pos mustEqual eastPos  } &&
        "move south" >> { ref.blocking.affect(MoveSouth); screenshot(pref + "3-south"); pos mustEqual southPos } &&
        "move west"  >> { ref.blocking.affect(MoveWest);  screenshot(pref + "4-west");  pos mustEqual westPos  }
    }
  }

  "Environment snapshots" should{
    "be accessible at all coordinates defined" in
      prop{ s: Environment#Snapshot =>  s.definedAt forall s.asEnv.get.isDefinedAt }
    "be correctly compared" in prop{
      ref: Environment#Ref =>
        val pos = ref.blocking.agentPosition(agentId).get
        val mapSnap = ref.worldSnapshot
        val moveTiles = mapSnap.getSnapshot(pos).neighboursSnapshots.filterNot(_.asAtom.exists(_.isHole))

        moveTiles.map{
          tile =>
            val moveCoord = tile.coordinate
            val isPlug = tile.asAtom.exists(_.isPlug)
            val snapshot0 = ref.blocking.snapshot
            val movement = mapSnap.asWorld.relativeNeighboursPosition(moveCoord, pos)
            val m1 = Move(movement)
            ref.blocking.affect(m1)
            val snapshot1 = ref.blocking.snapshot
            val m2 = Move(movement.opposite)
            ref.blocking.affect(m2)
            val snapshot2 = ref.blocking.snapshot

            val mustEqualSelf =
              (snapshot0 mustEqual snapshot0) and
              (snapshot1 mustEqual snapshot1) and
              (snapshot2 mustEqual snapshot2)

              mustEqualSelf and
                (ref.blocking.agentPosition(agentId).get mustEqual pos) and (
                if(isPlug)
                  (snapshot0 mustNotEqual snapshot2) and
                  (snapshot1 mustNotEqual snapshot0) and
                  (snapshot1 mustNotEqual snapshot2)
                else
                  (snapshot0 mustEqual snapshot2) and
                  (snapshot1 mustNotEqual snapshot0) and
                  (snapshot1 mustNotEqual snapshot2)
              )
        }.all
    }
  }

  def createTestsFolder() {   // todo
    val file = new File("tests")
    if(!file.exists()) file.mkdir()
  }
}
