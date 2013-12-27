import os
import sys

fileList = []
rootdir = "defaultrc"
enumFn = "enumerated.txt"

for root, subFolders, files in os.walk(rootdir):
  abridgedRoot = root[len(rootdir)+1:]
  for file in files:
    if file != enumFn:
      if abridgedRoot == "":
        path = file
      else:
        path = abridgedRoot + "/" + file
      fileList.append(path)

fileList.sort()

with open(os.path.join(rootdir, enumFn), 'wb') as f:
  for file in fileList:
    f.write((u"%s\n" % file).encode('utf-8'))
