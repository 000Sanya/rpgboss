package rpgboss.player

import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.lib.ThreadChecked
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import rpgboss.lib.GdxUtils

case class PartyBattler(project: Project, spriteSpec: SpriteSpec, x: Int,
                        y: Int) {
  val spriteset = Spriteset.readFromDisk(project, spriteSpec.name)

  val imageW = spriteset.tileW
  val imageH = spriteset.tileH
  
  def getBoundsArray() = Array(x, y, imageW, imageH)
}

/**
 * x and y coordinates refer to the top-left of the battler.
 */
case class EnemyBattlerInfo(x: Int, y: Int, w: Int, h: Int) {
  def getBoundsArray() = Array(x, y, w, h)
}

/**
 * This class must be created and accessed only on the Gdx thread.
 * 
 * @param   gameOpt   Is optional to allow BattleScreen use in the editor.
 */
class BattleScreen(
  gameOpt: Option[RpgGame],
  assets: RpgAssetManager,
  atlasSprites: TextureAtlas,
  project: Project,
  screenW: Int,
  screenH: Int)
  extends ThreadChecked
  with RpgScreen {
  assume(atlasSprites != null)

  object PlayerActionWindow extends ThreadChecked {
    class RunningWindow(battle: Battle, actor: BattleStatus) {
      import concurrent.ExecutionContext.Implicits.global
      
      assert(onBoundThread())
      assert(battle != null)
      
      def run() = concurrent.Future {
        assert(onDifferentThread())
        PlayerActionWindow.synchronized {
          current = Some(this)
        }
     
        val window = scriptInterface.newChoiceWindow(
          Array("Attack", "Skill", "Item"), 100, 300, 140, 180)
        
        window.getChoice() match {
          case 0 => {
            val target = getTarget(defaultToParty = false)
            GdxUtils.syncRun {
              _battle.get.takeAction(AttackAction(actor, target))
            }
          }
          case _ => {
            GdxUtils.syncRun {
              _battle.get.takeAction(NullAction(actor))
            }
          }
        }
        
        window.close()
      }
      
      /**
       * Gets the BattleStatus of a target in the battle.
       * TODO: Skip in-eligible targets (dead, etc.)
       */
      def getTarget(defaultToParty: Boolean) = {
        assert(onDifferentThread())
        assert(_enemyBattlers.length == _battle.get.enemyStatus.length)
        assert(_partyBattlers.length == _battle.get.partyStatus.length)
        
        val choices = 
          _enemyBattlers.map(_.getBoundsArray()) ++ 
          _partyBattlers.map(_.getBoundsArray())
        
        val defaultChoice = 
          if (defaultToParty)
            _enemyBattlers.length
          else 
            0
          
        val choice = scriptInterface.getSpatialChoice(
          choices.toArray, defaultChoice)
          
        if (choice < _enemyBattlers.length)
          _battle.get.enemyStatus(choice)
        else
          _battle.get.partyStatus(choice - _enemyBattlers.length)
      }
    }
    
    // Accessed on multiple threads
    var current: Option[RunningWindow] = None
    
    def spawnIfNeeded(battle: Battle,
                      readyEntity: Option[BattleStatus]) = synchronized {
      assert(onBoundThread())
      if (readyEntity.isDefined && current.isEmpty) {
        val window = new RunningWindow(_battle.get, readyEntity.get)
        window.run()
      }
    }
  }
  
  val scriptInterface = new ScriptInterface(gameOpt.orNull, this)

  val inputs = new InputMultiplexer()

  // Battle variables
  private var _battle: Option[Battle] = None
  private val _enemyBattlers = 
    new collection.mutable.ArrayBuffer[EnemyBattlerInfo]
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]

  val windowManager = new WindowManager(assets, project, screenW, screenH)

  def battleActive = _battle.isDefined
  
  val enemyListWindow = {
    if (gameOpt.isDefined) {
      new TextWindow(
        gameOpt.get.persistent,
        windowManager,
        inputs,
        Array(),
        0, 300, 200, 180)
    } else {
      null
    }
  }
  
  def updateEnemyListWindow() = {
    assert(_battle.isDefined)
    assert(enemyListWindow != null)
    val enemyLines = 
      Encounter.getEnemyLabels(_battle.get.encounter.units, project.data)
    enemyListWindow.updateText(enemyLines)
  }
  
  val partyListWindow = {
    if (gameOpt.isDefined) {
      new TextWindow(
        gameOpt.get.persistent,
        windowManager,
        inputs,
        Array(),
        200, 300, 440, 180)   
    } else {
      null
    }
  }
  
  def updatePartyListWindow() = {
    assert(_battle.isDefined)
    assert(partyListWindow != null)
    
    val partyLines = for (status <- _battle.get.partyStatus) yield {
      assert(status.id < _battle.get.pData.enums.characters.length)
      val name = _battle.get.pData.enums.characters(status.id).name
      val readiness = (math.min(status.readiness, 1.0) * 100).toInt
      "%-10s  %3d : %2d  %3d%%".format(name, status.hp, status.mp, readiness)
    }
    partyListWindow.updateText(partyLines)
  }
  
  def startBattle(battle: Battle, bgPicture: Option[String]) = {
    assert(onBoundThread())
    assert(_battle.isEmpty)

    bgPicture.map { picName =>
      // TODO: Make more robust
      windowManager.showPicture(PictureSlots.BATTLE_BACKGROUND, picName,
                                0, 0, 640, 320)
    }

    _battle = Some(battle)

    assert(_enemyBattlers.isEmpty)
    for ((unit, i) <- battle.encounter.units.zipWithIndex) {
      val enemy = project.data.enums.enemies(unit.enemyIdx)
      enemy.battler.map { battlerSpec =>
        val battler = Battler.readFromDisk(project, battlerSpec.name)
        val texture = battler.newGdxTexture

        val battlerWidth = (texture.getWidth() * battlerSpec.scale).toInt
        val battlerHeight = (texture.getHeight() * battlerSpec.scale).toInt
        
        val battlerLeft = unit.x - battlerWidth / 2 
        val battlerTop = unit.y - battlerHeight / 2

        windowManager.showTexture(
          PictureSlots.BATTLE_SPRITES_ENEMIES + i,
          texture,
          battlerLeft,
          battlerTop,
          battlerWidth,
          battlerHeight)
          
        _enemyBattlers.append(EnemyBattlerInfo(
          battlerLeft, battlerTop, battlerWidth, battlerHeight))
      }
    }

    assert(_partyBattlers.isEmpty)
    for ((partyId, i) <- battle.partyIds.zipWithIndex) {
      val character = project.data.enums.characters(partyId)
      character.sprite.map { spriteSpec =>
        val x = 10 * i + 550
        val y = 20 * i + 180
        _partyBattlers.append(PartyBattler(project, spriteSpec, x, y))
      }
    }
  }
  
  def endBattle() = {
    assert(onBoundThread())
    assert(_battle.isDefined)
    _battle = None

    for (i <- PictureSlots.BATTLE_BEGIN until PictureSlots.BATTLE_END) {
      windowManager.hidePicture(i)
    }

    _enemyBattlers.clear()
    _partyBattlers.clear()
  }

  def update(delta: Float): Unit = {
    if (windowManager.curTransition.isDefined)
      return
    
    _battle.map { battle =>
      PlayerActionWindow.spawnIfNeeded(battle, battle.readyEntity)
    }
    
    assert(onBoundThread())
    windowManager.update(delta)
  }

  def render() = {
    assert(onBoundThread())
    windowManager.render()

    windowManager.batch.begin()
    // TODO: Fix hack of re-using screenLayer's spritebatch
    for (partyBattler <- _partyBattlers) {
      GdxGraphicsUtils.renderSprite(
        windowManager.batch,
        atlasSprites,
        partyBattler.spriteset,
        partyBattler.spriteSpec.spriteIndex,
        SpriteSpec.Directions.WEST,
        SpriteSpec.Steps.STILL,
        partyBattler.x,
        partyBattler.y,
        partyBattler.imageW,
        partyBattler.imageH)
    }
    windowManager.batch.end()
  }

  /**
   * Dispose of any disposable resources
   */
  def dispose() = {
    assert(onBoundThread())

    if (battleActive)
      endBattle()

    windowManager.dispose()
  }
}
