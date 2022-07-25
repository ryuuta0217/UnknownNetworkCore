#ﾌﾝﾌﾝﾌﾝ常時実行ﾌﾝﾌﾝﾌﾝ
execute as @a if entity @s[nbt={Inventory: [{Slot: 103b, id: "minecraft:player_head", tag: {display: {Name:'{"text":"TomatoRocket","color":"dark_aqua","italic":false}'}}}]}] run effect give @s levitation 1 1 true
execute at @e[type=ender_pearl] run particle end_rod ~ ~ ~ 0.1 0.1 0.1 0 1 force