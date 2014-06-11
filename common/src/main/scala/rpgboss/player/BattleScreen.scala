package rpgboss.player

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.utils.Logger
import rpgboss.model._
import rpgboss.model.battle._
import rpgboss.model.Constants._
import rpgboss.model.resource._
import rpgboss.player.entity._
import rpgboss.lib._

case class PartyBattler(project: Project, spriteSpec: SpriteSpec, x: Int,
                        y: Int) extends BoxLike {
  val spriteset = Spriteset.readFromDisk(project, spriteSpec.name)

  val w = spriteset.tileW
  val h = spriteset.tileH
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
  
  val logger = new Logger("BatleScreen", Logger.INFO)

  object PlayerActionWindow extends ThreadChecked {
    import concurrent.ExecutionContext.Implicits.global
    
    class RunningWindow(battle: Battle, val actor: BattleStatus) {
      
      assert(onBoundThread())
      assert(battle != null)
      
      private var _window: ChoiceWindow#ChoiceWindowScriptInterface = null
      
      def close() = {
        assert(onBoundThread())
        concurrent.Future {
          _window.close()
        }
      }
      
      def run() = concurrent.Future {
        assert(onDifferentThread())
        PlayerActionWindow.synchronized {
          currentOpt = Some(this)
          
          _window = scriptInterface.newChoiceWindow(
            Array("Attack", "Skill", "Item"), 100, 300, 140, 180)
        }
     
        _window.getChoice() match {
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
        
        _window.close()
        
        PlayerActionWindow.synchronized {
          currentOpt = None
        }
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
    var currentOpt: Option[RunningWindow] = None
    
    def closeCurrentIfDead() = synchronized {
      assert(onBoundThread())
      
      // Closing needs to execute on script thread.
      currentOpt.map(current => {
        if (current.actor.hp <= 0) {
          // Closing will write a -1 to the choiceChannel and cause a NullAction
          // to be processed for this entity.
          current.close()
        }
      })
    }
    
    def spawnIfNeeded(battle: Battle,
                      readyEntity: Option[BattleStatus]) = synchronized {
      assert(onBoundThread())
      
      if (readyEntity.isDefined && currentOpt.isEmpty &&
          readyEntity.get.entityType == BattleEntityType.Party) {
        val window = new RunningWindow(_battle.get, readyEntity.get)
        window.run()
      }
    }
  }
  
  val scriptInterface = new ScriptInterface(gameOpt.orNull, this)

  val inputs = new InputMultiplexer()

  // Battle variables
  private var _battle: Option[Battle] = None
  private val _enemyBattlers = new collection.mutable.ArrayBuffer[IntRect]
  private val _partyBattlers = new collection.mutable.ArrayBuffer[PartyBattler]
  
  /**
   * Contains the state of the currently displaying battle notification.
   * This comprises of damage text, animations, etc.
   */
  case class NotificationDisplay(
    notification: BattleActionNotification,
    windows: Seq[Window]) {
    def done = windows.forall(_.state == Window.Closed)
  }
  var currentNotificationDisplay: Option[NotificationDisplay] = None
  
  def getBox(entityType: BattleEntityType.Value, id: Int): BoxLike = {
    assume(entityType == BattleEntityType.Party ||
        entityType == BattleEntityType.Enemy)
    
    if (entityType == BattleEntityType.Party) {
      return _partyBattlers(id)
    } else {
      return _enemyBattlers(id)
    }
  }

  val windowManager = new WindowManager(assets, project, screenW, screenH)

  def battleActive = _battle.isDefined

  val persistentState = gameOpt.map(_.persistent).getOrElse(new PersistentState)
  
  def getCharacterName(characterId: Int) = {
    assume(battleActive)
    val names = 
      persistentState.getStringArray(ScriptInterfaceConstants.CHARACTER_NAMES)
    if (characterId < names.length) {
      names(characterId)
    } else {
      assert(characterId < _battle.get.pData.enums.characters.length)
      _battle.get.pData.enums.characters(characterId).name
    }
  }
  
  def getEntityName(status: BattleStatus) {
    if (status.entityType == BattleEntityType.Party) {
      getCharacterName(status.id)
    } else {
      
    }
  }
  
  val enemyListWindow = {
    new TextWindow(
      persistentState,
      windowManager,
      inputs,
      Array(),
      0, 300, 200, 180)
  }
  
  def updateEnemyListWindow() = {
    assert(_battle.isDefined)
    assert(enemyListWindow != null)
    val enemyLines = 
      Encounter.getEnemyLabels(_battle.get.encounter.units, project.data)
    enemyListWindow.updateText(enemyLines)
  }
  
  val partyListWindow = {
    new TextWindow(
      persistentState,
      windowManager,
      inputs,
      Array(),
      200, 300, 440, 180)
  }
  
  def updatePartyListWindow() = {
    assert(_battle.isDefined)
    assert(partyListWindow != null)
    
    val partyLines = for (status <- _battle.get.partyStatus) yield {
      assert(status.id < _battle.get.pData.enums.characters.length)
      val name = getCharacterName(status.id)
      val readiness = (math.min(status.readiness, 1.0) * 100).toInt
      "%-10s  %3d : %2d  %3d%%".format(name, status.hp, status.mp, readiness)
    }
    partyListWindow.updateText(partyLines)
  }
  
  def startBattle(battle: Battle, battleBackground: String) = {
    assert(onBoundThread())
    assert(_battle.isEmpty)

    if (!battleBackground.isEmpty) {
      val bg = BattleBackground.readFromDisk(project, battleBackground)
      val texture = bg.newGdxTexture
      if (texture != null) {
        windowManager.showPicture(
          PictureSlots.BATTLE_BACKGROUND, 
          TexturePicture(texture, 0, 0, 640, 320))
      }
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

        windowManager.showPicture(
          PictureSlots.BATTLE_SPRITES_ENEMIES + i,
          TexturePicture(
            texture,
            battlerLeft,
            battlerTop,
            battlerWidth,
            battlerHeight))
          
        _enemyBattlers.append(
          IntRect(battlerLeft, battlerTop, battlerWidth, battlerHeight))
      }
    }

    assert(_partyBattlers.isEmpty)
    for ((partyId, i) <- battle.partyIds.zipWithIndex) {
      val character = project.data.enums.characters(partyId)
      character.sprite.map { spriteSpec =>
        val x = 10 * i + 550
        val y = 20 * i + 180
        val battler = PartyBattler(project, spriteSpec, x, y)
        
        val (srcX, srcY) = battler.spriteset.srcTexels(
          spriteSpec.spriteIndex, 
          SpriteSpec.Directions.WEST, 
          SpriteSpec.Steps.STEP0)
        
        windowManager.showPicture(
          PictureSlots.BATTLE_SPRITES_PARTY + i, 
          TextureAtlasRegionPicture(
            atlasSprites,
            battler.spriteset.name,
            battler.x,
            battler.y,
            battler.w,
            battler.h,
            srcX,
            srcY,
            battler.spriteset.tileW,
            battler.spriteset.tileH))
            
        _partyBattlers.append(battler)
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
    assert(onBoundThread())
    
    if (windowManager.curTransition.isDefined)
      return
    
    // All these actions should not take place if this is an in-editor session.
    if (gameOpt.isDefined) {
      _battle.map { battle =>
        battle.advanceTime(delta)
        
        // Dismiss the current notification if it's done.
        currentNotificationDisplay map { display =>
          if (display.done) {
            currentNotificationDisplay = None
            assert (battle.getNotification.isDefined)
            assert (display.notification == battle.getNotification.get)
            battle.dismissNotification()
          }
        }
        
        // Add the next notification if it exists.
        if (currentNotificationDisplay.isEmpty) {
          battle.getNotification.map { notification =>
            val windows = for (damage <- notification.damages) yield {
              val target = notification.action.target
              val box = getBox(target.entityType, target.id)
              new DamageTextWindow(gameOpt.get.persistent, windowManager, 
                  damage.value, box.x, box.y)
            }
            
            val display = NotificationDisplay(notification, windows)
            currentNotificationDisplay = Some(display)
          }
          
        }
        
        PlayerActionWindow.closeCurrentIfDead()
        PlayerActionWindow.spawnIfNeeded(battle, battle.readyEntity)
      }
    }
    
    updateEnemyListWindow()
    updatePartyListWindow()
    
    windowManager.update(delta)
  }

  def render() = {
    assert(onBoundThread())
    windowManager.render()
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
