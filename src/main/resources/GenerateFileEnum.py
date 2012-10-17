import os
import sys

fileList = []
rootdir = "defaultrc"
enumFn = "enumerated.txt"

for root, subFolders, files in os.walk(rootdir):
  abridgedRoot = root[len(rootdir)+1:]
  for file in files:
    if file != enumFn:
      fileList.append(abridgedRoot + "/" + file)

with open(os.path.join(rootdir, enumFn), 'wb') as f:
  for file in fileList:
    f.write("%s\n" % file)
