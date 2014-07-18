package rpgboss.player

import com.badlogic.gdx.Screen
import com.badlogic.gdx.Gdx
import rpgboss.lib.ThreadChecked
import rpgboss.model.SoundSpec
import aurelienribon.tweenengine._
import rpgboss.model.resource.{Music, MusicPlayer}


trait RpgScreen extends Screen with ThreadChecked {
  def game: RpgGame

  val scriptInterface = new ScriptInterface(game, this)

  val inputs = new InputMultiplexer()
  def createWindowManager(): WindowManager =
    new WindowManager(game.assets, game.project, game.screenWPx, game.screenHPx)

  val musics = Array.fill[Option[MusicPlayer]](8)(None)
  val windowManager = createWindowManager()

  val tweenManager = new TweenManager()

  def playMusic(slot: Int, specOpt: Option[SoundSpec],
    loop: Boolean, fadeDuration: Float) = {

    musics(slot).map({ oldMusic =>
      val tweenMusic = new MusicPlayerTweenable(oldMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDuration)
        .target(0f)
        .setCallback(new TweenCallback {
          override def onEvent(typeArg: Int, x: BaseTween[_]) = {
            if (typeArg == TweenCallback.COMPLETE) {
              oldMusic.stop()
            }
          }
        }).start(tweenManager)
    })

    musics(slot) = specOpt.map { spec =>
      val resource = Music.readFromDisk(game.project, spec.sound)
      val newMusic = resource.newPlayer(game.assets)

      // Start at zero volume and fade to desired volume
      newMusic.stop()
      newMusic.setVolume(0f)
      newMusic.setLooping(loop)
      newMusic.play()

      // Setup volume tween
      val tweenMusic = new MusicPlayerTweenable(newMusic)
      Tween.to(tweenMusic, GdxMusicAccessor.VOLUME, fadeDuration)
        .target(spec.volume).start(tweenManager)

      newMusic
    }
  }

  def render()
  def update(delta: Float)

  override def dispose() = {
    windowManager.dispose()

    musics.foreach(_.map(music => {
      music.stop()
      music.dispose()
    }))
  }

  override def hide() = {
    assertOnBoundThread()
    Gdx.input.setInputProcessor(null)

    musics.foreach(_.map(_.pause()))
  }

  override def pause() = {
    assertOnBoundThread()
  }

  override def render(delta: Float) = {
    assertOnBoundThread()

    // Update tweens
    tweenManager.update(delta)

    update(delta)
    render()
  }

  override def resize(width: Int, height: Int) = {
    assertOnBoundThread()
    // Do nothing for now
  }

  override def resume() = {
    assertOnBoundThread()
  }

  override def show() = {
    assertOnBoundThread()

    Gdx.input.setInputProcessor(inputs)
    musics.foreach(_.map(_.play()))
  }
}
