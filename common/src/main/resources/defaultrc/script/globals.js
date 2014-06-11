function teleportLoc(loc, transition) {
  var map = game.getMap(loc);
  var fadeDuration = Transitions.fadeLength();
  
  if (map.metadata().changeMusicOnEnter()) {
    game.playMusic(0, map.metadata().music(), true, fadeDuration);
  }
  
  if (Transitions.get(transition) == Transitions.FADE()) {
    game.setTransition(1, 0, fadeDuration);
    game.sleep(fadeDuration);
  }

  game.setPlayerLoc(loc);

  if (Transitions.get(transition) == Transitions.FADE()) {
    game.setTransition(0, 1, fadeDuration);
  }
}

function teleport(mapName, x, y, transition) {
  return teleportLoc(MapLoc.apply(mapName, x, y), transition);
}

function showText(text) {
  game.showText(text, 0 /* x */, 300 /* y */, 640 /* w */, 180 /* h */, 
                0.02 /* timePerChar */); 
}

// Casting to support both Javascript and Java strings.
function leftPad(string, totalLen) {
  var castedString = String(string);
  return Array(totalLen - castedString.length).join(" ") + castedString;
}

function rightPad(string, totalLen) {
  var castedString = String(string);
  return castedString + Array(totalLen - castedString.length).join(" ");
}