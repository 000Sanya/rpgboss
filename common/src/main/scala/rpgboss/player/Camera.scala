package rpgboss.player

import scala.concurrent._
import scala.concurrent.duration.Duration
import com.badlogic.gdx.math.Vector2

case class CameraInfo(x: Float, y: Float, speed: Float, moveQueueLength: Int)

/** Controls where the camera is pointed. Accessed only on the Gdx thread.
 *  
 *  The camera is implicitly locked to the player. If however, there are moves
 *  in the move queue, it will execute those before re-locking to the player.
 *  
 *  Callers must move the camera back to the player if they don't want the
 *  camera to jerk afterwards.
 */
class Camera(game: MyGame) {
  var x: Float = 0f
  var y: Float = 0f
  var speed: Float = 2f // tiles per second
  val moveQueue = new collection.mutable.Queue[CameraMoveTrait]
  
  def state = game.state
  
  def info = CameraInfo(x, y, speed, moveQueue.length)
  
  def update(delta: Float) = {
    if (moveQueue.isEmpty) {
      x = game.state.playerEntity.x
      y = game.state.playerEntity.y
    } else if (!moveQueue.isEmpty) {
      val move = moveQueue.head

      moveQueue.head.update(delta, this)
      
      if (moveQueue.head.isDone())
        moveQueue.dequeue()
    }
  }
  
  def enqueueMove(dx: Float, dy: Float): CameraMove = {
    val move = new CameraMove(dx, dy)
    moveQueue.enqueue(move)
    return move
  }
}

trait CameraMoveTrait {
  private val finishPromise = Promise[Int]()
  
  def update(delta: Float, c: Camera)
  
  def isDone() = finishPromise.isCompleted
  def finish() = finishPromise.success(0)
  def awaitDone() = Await.result(finishPromise.future, Duration.Inf)
}

class CameraMove(dx: Float, dy: Float) extends CameraMoveTrait {
  private val _remaining = new Vector2(dx, dy)

  def update(delta: Float, c: Camera) = {
    val maxTravel = delta * c.speed
    if (_remaining.len() <= maxTravel) {
      c.x += _remaining.x
      c.y += _remaining.y
      finish()
    } else {
      val travel = _remaining.nor().scl(maxTravel)
      c.x += travel.x
      c.y += travel.y
      _remaining.sub(travel)
    }
  }
}