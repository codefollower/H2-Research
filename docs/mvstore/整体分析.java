org.h2.mvstore.MVStore的MVMapConcurrent<String, String> meta存放哪些key/value：

key                            value
=============================================
"map."+ mapId                  mapName
"name." + mapName              mapId

"chunk." + chunkId             Chunk.asString()
"root." + mapId                pagePos
"setting.storeVersion"         storeVersion
"rollbackOnOpen"               lastCommittedVersion

