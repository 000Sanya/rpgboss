function StoreBuyMenu(itemIds, priceMultiplier) {
  var menu = new Menu({
    getState : function() {
      var choiceLines = [];
      var itemPrices = [];
      var items = project.data().enums().items();
      for (var i = 0; i < itemIds.length; ++i) {
        var itemId = itemIds[i];
        var item = items[itemId];
        var price = item.price() * priceMultiplier;
        itemPrices.push(price);

        var lineParts = [];
        lineParts.push(rightPad(item.name(), 20));
        lineParts.push(leftPad(price.toString(), 5));
        
        choiceLines.push(lineParts.join(""));
      }

      return {
        lines : choiceLines,
        itemIds : itemIds,
        itemPrices: itemPrices
      }
    },
    layout : layout.southwest(sizer.prop(0.6, 0.87)),
    windowDetails : {
      displayedItems : 10,
      allowCancel : true
    }
  });

  return menu;
}

function openStore(itemIdsSold, buyPriceMultiplier, sellPriceMultiplier) {
  var items = project.data().enums().items();

  var storeHeaderWin = game.newStaticTextWindow(
    ["Store"],
    layout.northwest(sizer.prop(0.5, 0.13)),
    { justification : game.CENTER() });
  
  var storeRightPane = game.newStaticTextWindow(
    [], layout.southeast(sizer.prop(0.4, 0.87)), {})
    
  function updateStoreRightPane(itemId) {
    lines = [];
    lines.push("Gold: ");
    lines.push("  "  + game.getInt(Constants.GOLD()));
    
    if (itemId >= 0) {
      lines.push("Owned: ");
      lines.push("  " + game.countItems(itemId));
    }
    storeRightPane.updateLines(lines)
  }
  updateStoreRightPane(-1);
  
  var storeTopWin = new Menu({
    getState : function() {
      return {
        lines : ["Buy", "Sell"]
      };
    },
    layout : layout.northeast(sizer.prop(0.5, 0.13)),
    windowDetails : {
      justification : game.CENTER(),
      columns : 2,
      allowCancel : true
    }
  });

  storeTopWin.loopChoice(function(choiceId) {
    if (choiceId == 0) {
      var buyMenu = new StoreBuyMenu(itemIdsSold, buyPriceMultiplier);
      buyMenu.loopChoice(function(choiceId) {
        var itemId = itemIdsSold[choiceId];
        var item = items[itemId];
        
        if (game.addRemoveGold(-item.price())) {
          game.addRemoveItem(itemId, 1);
        }
        
        updateStoreRightPane(itemId);
        return true;
      });
      buyMenu.close();
    } else {
      var itemsMenu = 
        new ItemMenu(false, layout.southwest(sizer.prop(0.6, 0.87)), 20);
      itemsMenu.loopChoice(function(choiceId) {
        if (itemsMenu.state.itemIds.length == 0)
          return false;
        
        var itemId = itemsMenu.state.itemIds[choiceId];
        var item = items[itemId];
               
        var itemsLeft = itemsMenu.state.itemQtys[choiceId];
        
        if (itemsLeft >= 1) {
          game.addRemoveGold(item.price() * sellPriceMultiplier);
          game.addRemoveItem(itemId, -1);
          updateStoreRightPane(itemId);
        }
        
        return true;
      });
    }
    return true;
  });
  
  storeRightPane.close();
  storeTopWin.close();
  storeHeaderWin.close();
}