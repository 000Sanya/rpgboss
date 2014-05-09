package rpgboss.model.battle

import rpgboss.model._

/**
 * A BattleAction is an action taken by an entity in the battle. It consumes 
 * the initiative a.k.a. readiness of that entity and moves it to the back
 * of the ready queue.
 */
trait BattleAction {
  def actor: BattleStatus
  def process(battle: Battle)
}

case class NullAction(actor: BattleStatus) extends BattleAction {
  def process(battle: Battle) = {}
}

case class AttackAction(actor: BattleStatus, target: BattleStatus)
  extends BattleAction {
  def process(battle: Battle) = {
    val allItems = battle.pData.enums.items
    val weaponSkills = 
      actor.equipment
        .filter(_ < allItems.length)
        .map(id => allItems(id).onUseSkillId)
    val damages = 
      weaponSkills
        .map(skillId => Damage.getDamages(actor, target, battle.pData, skillId))
        .flatten
    target.hp -= damages.map(_.value).sum
  }
}

case class SkillAction(actor: BattleStatus, target: BattleStatus, skillId: Int)
  extends BattleAction {
  def process(battle: Battle): Unit = {
    if (skillId < battle.pData.enums.skills.length)
      return
    
    val skill = battle.pData.enums.skills(skillId)
    if (actor.mp < skill.cost)
      return
      
    actor.mp -= skill.cost
      
    val damages = Damage.getDamages(actor, target, battle.pData, skillId)
    target.hp -= damages.map(_.value).sum
  }
}

