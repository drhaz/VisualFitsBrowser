# VisualFitsBrowser

VisualFitsBrowser is a small utility program to help organizing astronomical images in the FITS format, and
his meant to assist a workflow of inspecting images in saoimage ds9 in an observatory or lab environment.

The center of the application is a list view of all fits images in a directory, sortable by some selected
fits header keywords (OBJECT, DATE-OBS, FILTER). Double-clicking on one file will send the image for display
to ds9. Furthermore, there is a capability to store comments for each image and then to generate a nice looking logfile 
via a local latex installation.

More detailed documentation is provided at https://github.com/drhaz/VisualFitsBrowser/wiki

## Installation & Download
To install:
Download the latest release .jar file from the Release folder. The direct link to version 1.6 should be:

https://github.com/drhaz/VisualFitsBrowser/blob/master/Releases/VisualFitsBrowser-1.6-jar-with-dependencies.jar?raw=true


To start the VisualFitsBrowser, execute on your computer:

 java -jar VisualFitsBrowser-1.6-jar-with-dependencies.jar 

replace the version number / name of the jar file with the file you actually downloaded. 
