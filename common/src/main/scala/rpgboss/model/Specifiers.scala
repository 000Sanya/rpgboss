package rpgboss.model

import Constants._

trait HasName {
  def name: String
}

object HasName {
  /**
   * Allows Strings to be used in functions that take HasName arguments.
   */
  implicit def StringToHasName(x: String) = new HasName {
    def name = x
  }
}

/**
 * These locations are given with the top-left of the map being at (0, 0).
 * This means that the center of the tiles are actually at 0.5 intervals.
 */
case class MapLoc(
  map: String = "",
  var x: Float = 0f,
  var y: Float = 0f)

object WhichEntity extends RpgEnum {
  val PLAYER = Value(0, "Player")
  val THIS_EVENT = Value(1, "This event")
  val EVENT_ON_MAP = Value(2, "Event on Map")
  val EVENT_ON_OTHER_MAP = Value(3, "Event on Other Map")

  def default = THIS_EVENT
}

object Scope extends RpgEnum {
  val None = Value
  val SelfOnly = Value("User only")
  val OneEnemy = Value("One enemy")
  val AllEnemies = Value("All enemies")
  val OneAlly = Value("One ally")
  val AllAllies = Value("All allies")
  val OneAllyDead = Value("One dead ally")
  val AllAlliesDead = Value("All dead allies")

  def default = OneAlly
}

object AddOrRemove extends RpgEnum {
  val Add, Remove = Value
  def default = Add

  def fromBoolean(x: Boolean) = if (x) Add else Remove
  def toBoolean(id: Int) = id == Add.id
}

case class BattlerSpec(var name: String, var scale: Float = 1.0f)

// Specifies an entity: Either the player or an event on the current map.
case class EntitySpec(
  var whichEntityId: Int = WhichEntity.default.id,
  var mapName: String = "",
  var eventId: Int = -1)

case class IconSpec(iconset: String, iconX: Int, iconY: Int)

case class RandomEncounter(var encounterId: Int = 0, var weight: Int = 10)

case class RandomEncounterSettings(
  var stepsAverage: Int = 20,
  var encounters: Array[RandomEncounter] = Array()) {
  def deepcopy() = copy(encounters = encounters.map(_.copy()))
}

case class SoundSpec(
  sound: String = "",
  var volume: Float = 1.0f,
  var pitch: Float = 1.0f)

case class SpriteSpec(
  name: String,
  spriteIndex: Int,
  dir: Int = SpriteSpec.Directions.SOUTH,
  step: Int = SpriteSpec.Steps.STILL)

object SpriteSpec {
  object Directions {
    val SOUTH = 0
    val WEST = 1
    val EAST = 2
    val NORTH = 3
    val NONE = -1

    def opposite(dir: Int) = dir match {
      case SOUTH => NORTH
      case WEST => EAST
      case EAST => WEST
      case NORTH => SOUTH
      case _ => NONE
    }
  }

  object Steps {
    val STEP0 = 0
    val STEP1 = 1
    val STEP2 = 2
    val STEP3 = 3

    val STILL = 1

    val TOTALSTEPS = 4
  }
}
