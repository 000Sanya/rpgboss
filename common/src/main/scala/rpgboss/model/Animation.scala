package rpgboss.model

case class AnimationKeyframe(
  var time: Float = 0.0f,
  var frameIndex: Int = 0,
  var x: Int = 0,
  var y: Int = 0)

case class AnimationVisual(
  var animationImage: String = "",
  var start: AnimationKeyframe = AnimationKeyframe(),
  var end: AnimationKeyframe = AnimationKeyframe())

case class AnimationSound(
  var time: Float = 0.0f,
  var sound: SoundSpec = SoundSpec())

case class Animation(
  var name: String = "Animation",
  var visuals: Array[AnimationVisual] = Array(),
  var sounds: Array[AnimationSound] = Array()) extends HasName {

  def totalTime = math.max(visuals.map(_.end.time).max, sounds.map(_.time).max)
}