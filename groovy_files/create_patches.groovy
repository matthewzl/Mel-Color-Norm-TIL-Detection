/**
 * Script for saving image into tiles for more workable
 * downstream processing.
 * 
 */


if (getAnnotationObjects().size() == 0) {
   println("NO ANNOTATION DETECTED. USING FULL IMAGE...")
   def server = getCurrentImageData().getServer()
   def plane = ImagePlane.getPlane(0, 0)
   def roi = ROIs.createRectangleROI(0, 0, server.getWidth(),server.getHeight(), plane)
   def annotation = PathObjects.createAnnotationObject(roi)
   addObject(annotation)
}

// OPTIONALLY SET OUTPUT DIRECTORY HERE
File dir = Projects.getBaseDirectory(getQuPath().getProject())

File rootDir = new File(dir.getAbsolutePath() + "/tiles")
if (!rootDir.exists()) {
    rootDir.mkdirs()
}

mergeAnnotations(getAnnotationObjects())
originalAnnotation = getAnnotationObjects()[0]
originalAnnotation.setName(null)

selectObjects(getAnnotationObjects()[0])
runPlugin('qupath.lib.algorithms.TilerPlugin', '{"tileSizeMicrons":5000.0,"trimToROI":true,"makeAnnotations":true,"removeParentAnnotation":false}')

def pixelHeight = getCurrentServer().getPixelCalibration().getPixelHeight()
def pixelWidth = getCurrentServer().getPixelCalibration().getPixelWidth()
def zSpacing = getCurrentServer().getPixelCalibration().getZSpacing()

List<PathObject> tiles = getAnnotationObjects().findAll { !it.getName().equals(null)}

String imageName = getImageBaseName()
def server = getCurrentServer()

File subDir;
tiles.parallelStream().forEach { tile ->
    def roi = tile.getROI()
    def region = RegionRequest.createInstance(server.getPath(), 1, roi)
//    print tile
//    print roi
//    print region.getMinY()

    String outputPath = "${imageName}_[x-${region.getMinX()},y-${region.getMinY()},w-${region.getWidth()},h-${region.getHeight()}]_[pHeight-${pixelHeight},pWidth-${pixelWidth},zSp-${zSpacing}]"
  
    subDir = new File(rootDir.getAbsolutePath() + "/${imageName}")
    subDir.mkdirs()
    File file = new File(subDir, "${outputPath}.tif")
    println("Writing ${outputPath}") 
    ImageWriterTools.writeImageRegion(server, region, file.toString())
}

// Write the reference image (NOT USED IN THIS SCRIPT)
// def refRoi = originalAnnotation.getROI()
// int MAX_PIXELS = 8000*8000
// double downsample = Math.max(1, Math.sqrt(refRoi.getArea()/MAX_PIXELS))
// def refRegion = RegionRequest.createInstance(server.getPath(), downsample, refRoi)
// File refFile = new File(subDir, "reference.tif")
// println("Writing reference image...") 
// ImageWriterTools.writeImageRegion(server, refRegion, refFile.toString())

println("Done")


static String getImageBaseName() {
    def imageData = getCurrentImageData()
    def name = GeneralTools.getNameWithoutExtension(imageData.getServer().getMetadata().getName())
    def pathOutput = buildFilePath(PROJECT_BASE_DIR, 'tiles', name)
    File imageFile = new File(pathOutput)
    
    return imageFile.getName()
}