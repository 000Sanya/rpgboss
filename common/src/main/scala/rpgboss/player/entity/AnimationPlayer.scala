package rpgboss.player.entity

import com.badlogic.gdx.utils.Disposable
import rpgboss.lib._
import rpgboss.model._
import rpgboss.model.resource._
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import rpgboss.player.GdxGraphicsUtils

/**
 * Can only be used from the Gdx thread.
 */
class AnimationPlayer(
  proj: Project, animation: Animation, assets: RpgAssetManager,
  dstXOffset: Float, dstYOffset: Float)
  extends Disposable {

  case class SoundState(
    animationSound: AnimationSound, resource: Sound, var played: Boolean)

  // Load all the assets used in this animation.
  val animationImages = animation.visuals.map(
    v => AnimationImage.readFromDisk(proj, v.animationImage))
  val animationSounds = animation.sounds.map(s => {
    val sound = Sound.readFromDisk(proj, s.sound.sound)
    sound.loadAsset(assets)

    SoundState(s, sound, false)
  })

  animationImages.map(_.loadAsset(assets))

  /**
   *  Time of the previous update call.
   */

  private var _time = 0.0f
  private var _playing = false

  def allResourcesLoaded = {
    animationImages.forall(_.isLoaded(assets)) &&
      animationSounds.forall(_.resource.isLoaded(assets))
  }

  def time = _time
  def playing = _playing

  def play() = _playing = true

  var done = false

  def update(delta: Float): Unit = {
    if (!allResourcesLoaded)
      return

    if (_playing) {
      _time += delta
      if (_time >= animation.totalTime) {
        _time = 0f
        _playing = false
        done = true
      }
    }
  }

  /**
   * Assumes |batch| is already centered on the animation origin.
   */
  def render(batch: SpriteBatch) = {
    import TweenUtils._
    for ((visual, image) <- animation.visuals zip animationImages) {
      if (visual.within(time) && image.isLoaded(assets)) {
        val alpha = tweenAlpha(visual.start.time, visual.end.time, time)
        val frameIndex = tweenIntInclusive(
          alpha, visual.start.frameIndex, visual.end.frameIndex)

        val dstX = dstXOffset + tweenFloat(alpha, visual.start.x, visual.end.x)
        val dstY = dstYOffset + tweenFloat(alpha, visual.start.y, visual.end.y)

        val xTile = frameIndex % image.xTiles
        val yTile = frameIndex / image.xTiles

        image.drawTileCentered(batch, assets, dstX, dstY, xTile, yTile)
      }
    }

    for (soundState <- animationSounds) {
      if (!soundState.played && soundState.animationSound.time >= time &&
          soundState.resource.isLoaded(assets)) {
        val soundSpec = soundState.animationSound.sound
        soundState.resource.getAsset(assets).play(
          soundSpec.volume, soundSpec.pitch, 0f)
        soundState.played = true
      }
    }
  }

  def dispose() = {
    // TODO: Sounds are currently cut off due to AnimationPlayer being disposed
    // prematurely, since there is no way to tell if a sound is finished playing
    // or not...

    animationImages.map(_.unloadAsset(assets))
    animationSounds.map(_.resource.unloadAsset(assets))
  }
}