# Icon sources

Vector sources for icons in `src/main/java/resources/resources/icons/`.

To regenerate a PNG with a transparent background (macOS, Chrome and ImageMagick):

```sh
"/Applications/Google Chrome.app/Contents/MacOS/Google Chrome" --headless=new --disable-gpu \
    --hide-scrollbars --default-background-color=00000000 --window-size=512,512 \
    --screenshot=$PWD/raw_cow_512.png "file://$PWD/raw_cow.svg"
magick raw_cow_512.png -resize 128x128 ../../src/main/java/resources/resources/icons/raw_cow.png
```
