#!/bin/bash

cd iphone && rm -rf build/* && rm -f *.zip && /usr/bin/python build.py 
for f in *.zip; do
  mv "$f" "../release/$f"
done 

cd ../

cd android && /usr/local/bin/ant clean && /usr/local/bin/ant
cd dist
for f in *.zip; do
  mv "$f" "../../release/$f"
done
cd ../

cd ../
