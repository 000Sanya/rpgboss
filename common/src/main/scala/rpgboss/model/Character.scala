package rpgboss.model

/**
 * @param startingEquipment   Denotes the item ids of starting equipment.
 *                            A value of -1 means it's an empty slot.
 *
 * @param equipFixed          "true" means the player cannot modify this slot.
 */
case class Character(
  var name: String = "",
  var subtitle: String = "",
  var description: String = "",
  var sprite: Option[SpriteSpec] = None,
  var initLevel: Int = 1, var maxLevel: Int = 50,
  var charClass: Int = 0,
  var progressions: CharProgressions = CharProgressions(),
  var startingEquipment: Seq[Int] = Seq(),
  var equipFixed: Seq[Int] = Seq()) extends HasName {
  def initMhp = progressions.mhp(initLevel)
  def initMmp = progressions.mmp(initLevel)
}

case class CharClass(
  var name: String = "",
  var canUseItems: Seq[Int] = Seq(),
  var effects: Seq[Effect] = Seq()) extends HasName

case class Enemy(
  var name: String = "",
  var battler: Option[BattlerSpec] = None,
  var level: Int = 5,
  var mhp: Int = 40,
  var mmp: Int = 40,
  var atk: Int = 10,
  var spd: Int = 10,
  var mag: Int = 10,
  var expValue: Int = 100,
  var effects: Seq[Effect] = Seq()) extends HasName
  
case class EncounterUnit(
  enemyIdx: Int,
  var x: Int,
  var y: Int)
  
case class Encounter(
  var name: String = "#<None>",
  var units: Seq[EncounterUnit] = Seq())

case class Skill(name: String = "") extends HasName

object CharState {
  /*
   * val defaultStates = 
      CharState("Dead",      Map("NoAction"->1)),
      CharState("Stunned",   Map("NoAction"->1),     1, 100),
      CharState("Berserk",   Map("AutoAtkEnemy"->1,
                                 "AtkMul"-> 75),     8, 25),
      CharState("Poisoned",  Map("HpRegenMul"-> -5), 8, 50,  0,  3),
      CharState("Mute",      Map("NoMagic"->1),      2, 100),
      CharState("Confused",  Map("AutoAtkAlly"->1),  3, 50,  50),
      CharState("Asleep",    Map("NoAction"->1),     6, 50,  100),
      CharState("Paralyzed", Map("NoAction"->1),     3, 25,  25),
      CharState("Blinded",   Map("DexMul"-> -50),    8, 50),
      CharState("Weakened",  Map("AtkMul"-> -30),    4, 100, 0,  3),
      CharState("Hasted",    Map("DexMul"-> 50),     4, 100, 0,  2),
      CharState("Slowed",    Map("DexMul"-> -50),    4, 100, 0,  2)
  )*/

}
