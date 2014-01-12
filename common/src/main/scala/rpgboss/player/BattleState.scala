package rpgboss.player

import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._

case class PartyBattler(spriteSpec: SpriteSpec)

class BattleState(game: MyGame, project: Project) {
  // Battle variables
  private var _battle: Option[Battle] = None
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]
  
  def battleActive = _battle.isDefined
  
  def startBattle(battle: Battle, bgPicture: Option[String]) = {
    assert(_battle.isEmpty)
    
    bgPicture.map { picName =>
      // TODO: Make more robust
      game.scriptInterface.showPicture(PictureSlots.BATTLE_BACKGROUND, picName, 
                                       0, 0, 640, 320)
    }
    
    _battle = Some(battle)
    
    for ((unit, i) <- battle.encounter.units.zipWithIndex) {
      val enemy = project.data.enums.enemies(unit.enemyIdx)
      enemy.battler.map { battlerSpec =>
        val battler = Battler.readFromDisk(project, battlerSpec.name)
        val texture = battler.newGdxTexture
        game.scriptInterface.showTexture(
          PictureSlots.BATTLE_SPRITES_ENEMIES + i, 
          texture, 
          unit.x, 
          unit.y, 
          (texture.getWidth() * battlerSpec.scale).toInt, 
          (texture.getHeight() * battlerSpec.scale).toInt)
      }
    }
    
    for ((partyId, i) <- battle.partyIds.zipWithIndex) {
      val character = project.data.enums.characters(partyId)
      character.sprite.map { spriteSpec =>
        val x = 600
        val y = 40 * i + 100
        _partyBattlers.append(PartyBattler(spriteSpec))
      }
    }
  }
  
  def endBattle() = {
    assert(_battle.isDefined)
    _battle = None
    
    for (i <- PictureSlots.BATTLE_BEGIN until PictureSlots.BATTLE_END) {
      game.scriptInterface.hidePicture(i)
    }
    
    _partyBattlers.clear()
  }
     
  def update(delta: Float) = {
  }

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
  }
}
