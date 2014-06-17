package rpgboss.player
import collection.mutable._
import collection.mutable.{HashMap => MutableHashMap}
import rpgboss.lib.ThreadChecked

trait PersistentStateUpdate
case class IntChange(key: String, value: Int) extends PersistentStateUpdate
case class EventStateChange(key: (String, Int), value: Int)
  extends PersistentStateUpdate

/**
 * The part of the game state that should persist through save and load cycles.
 * This whole class should only be accessed on the Gdx thread.
 */
class PersistentState
  extends ThreadChecked
  with Publisher[PersistentStateUpdate] {
  private val globalInts = new MutableHashMap[String, Int]
  private val intArrays = new MutableHashMap[String, Array[Int]]
  private val stringArrays = new MutableHashMap[String, Array[String]]

  // mapName->eventId->state
  val eventStates =
    new MutableHashMap[(String, Int), Int]
    with ObservableMap[(String, Int), Int]

  // TODO: save player location
  def setInt(key: String, value: Int) = {
    assertOnBoundThread()
    globalInts.update(key, value)
    publish(IntChange(key, value))
  }
  def getInt(key: String) = {
    assertOnBoundThread()
    globalInts.get(key).getOrElse(-1)
  }

  /**
   * Returns a defensive copy of the stored array.
   */
  def getIntArray(key: String) = {
    assertOnBoundThread()
    intArrays.get(key).map(_.clone()).getOrElse(new Array[Int](0))
  }

  def setIntArray(key: String, value: Array[Int]) = {
    assertOnBoundThread()
    intArrays.update(key, value.toArray)
  }

  /**
   * Returns a defensive copy of the stored array.
   */
  def getStringArray(key: String) = {
    assertOnBoundThread()
    stringArrays.get(key).map(_.clone()).getOrElse(new Array[String](0))
  }

  def setStringArray(key: String, value: Array[String]) = {
    assertOnBoundThread()
    stringArrays.update(key, value.toArray)
  }

  // Gets the event state for the current map.
  // Returns zero if none is saved.
  def getEventState(mapName: String, eventId: Int) = {
    assertOnBoundThread()
    eventStates.get((mapName, eventId)).getOrElse(0)
  }

  def setEventState(mapName: String, eventId: Int, newState: Int) = {
    assertOnBoundThread()
    eventStates.update((mapName, eventId), newState)
  }
}

object PersistentStateUtils extends HasScriptConstants {
  /**
   * Returns list of characters that leveled up by their character index.
   */
  def givePartyExperience(persistent: PersistentState, 
                          characters: Array[rpgboss.model.Character], 
                          partyIds: Array[Int], 
                          experience: Int) = {
    val levels = persistent.getIntArray(CHARACTER_LEVELS)
    val exps = persistent.getIntArray(CHARACTER_EXPS)
    
    assert(levels.length == characters.length)
    assert(exps.length == characters.length)
    
    val leveledBuffer = collection.mutable.ArrayBuffer[Int]()
    for (i <- partyIds) {
      val character = characters(i)
      exps(i) += experience
      
      var leveled = false
      while (exps(i) >= character.expToLevel(levels(i))) {
        exps(i) -= character.expToLevel(levels(i))
        levels(i) += 1
        
        leveled = true
      }
      
      if (leveled) {character.expToLevel(levels(i))
        leveledBuffer += i
      }
    }
    
    persistent.setIntArray(CHARACTER_LEVELS, levels)
    persistent.setIntArray(CHARACTER_EXPS, exps)
    
    leveledBuffer.toArray
  }
}