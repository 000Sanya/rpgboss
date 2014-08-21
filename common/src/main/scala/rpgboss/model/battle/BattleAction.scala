package rpgboss.model.battle

import rpgboss.model._

case class Hit(hitActor: BattleStatus, damages: Array[TakenDamage],
               animationIds: Array[Int])

/**
 * A BattleAction is an action taken by an entity in the battle. It consumes
 * the initiative a.k.a. readiness of that entity and moves it to the back
 * of the ready queue.
 */
trait BattleAction {
  def actor: BattleStatus
  def targets: Array[BattleStatus]

  def process(battle: Battle): Array[Hit]
}

case class NullAction(actor: BattleStatus) extends BattleAction {
  override def targets = Array()
  def process(battle: Battle) = {
    Array()
  }
}

case class AttackAction(actor: BattleStatus, targets: Array[BattleStatus])
  extends BattleAction {
  def process(battle: Battle) = {
    assert(targets.length == 1)
    val desiredTarget = targets.head

    val actualTargets =
      if (desiredTarget.alive)
        Some(desiredTarget)
      else
        battle.randomAliveOf(desiredTarget.entityType)

    val hitOption = for (target <- actualTargets) yield {
      val damages =
        actor.onAttackSkillIds
          .map(skillId =>
            Damage.getDamages(actor, target, battle.pData, skillId))
          .flatten

      target.hp -= math.min(target.hp, damages.map(_.value).sum)

      val animations = actor.onAttackSkillIds.map(skillId => {
        assert(skillId < battle.pData.enums.skills.length)
        val skill = battle.pData.enums.skills(skillId)
        skill.animationId
      })

      Hit(target, damages, animations)
    }

    hitOption.toArray
  }
}

case class SkillAction(actor: BattleStatus, targets: Array[BattleStatus],
                       skillId: Int)
  extends BattleAction {
  def process(battle: Battle) = {
    if (skillId < battle.pData.enums.skills.length)
      Array()

    val skill = battle.pData.enums.skills(skillId)
    if (actor.mp < skill.cost)
      Array()

    actor.mp -= skill.cost

    assert(targets.length >= 1)

    for (target <- targets) yield {
      val damages = Damage.getDamages(actor, target, battle.pData, skillId)
      target.hp -= damages.map(_.value).sum

      if (target.hp < 0)
        target.hp = 0
      else if (target.hp > target.stats.mhp)
        target.hp = target.stats.mhp

      Hit(target, damages, Array(skill.animationId))
    }
  }
}

