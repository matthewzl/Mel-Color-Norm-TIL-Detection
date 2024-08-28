/**
 * Script for stitching tiles.
 *
 * <p>Acknowledgment:</p>
 * Parts of this code were adapted from a script by Pete Bankhead.
 * The original script can be found at:
 * <a href="https://gist.github.com/petebankhead/b5a86caa333de1fdcff6bdee72a20abe">
 * https://gist.github.com/petebankhead/b5a86caa333de1fdcff6bdee72a20abe</a>.
 * 
 */


import qupath.lib.images.servers.SparseImageServer
import qupath.lib.images.servers.ImageServerProvider
import qupath.lib.images.servers.ImageServers
import java.text.SimpleDateFormat
import qupath.lib.images.writers.ome.OMEPyramidWriter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import qupath.lib.gui.dialogs.Dialogs

final Logger logger = LoggerFactory.getLogger("ScriptEditor")

File inputDir = Dialogs.promptForDirectory("Choose Input Directory", null)
if (inputDir == null) {
    println("No input directory selected.")
    return
}

File outputDir = Dialogs.promptForDirectory("Choose Output Directory", null)
if (outputDir == null) {
    println("No output directory selected.")
    return
}

List<File> patchDirectories = []
inputDir.listFiles().each { file ->
    if (file.isDirectory()) {
        patchDirectories << file
    }
}

for (File patchDirectory : patchDirectories) {

    try {

        println("Stitching patches in ${patchDirectory.getName()}...")

        List<String> validExtensions = [".tif", ".tiff", ".jpg", ".jpeg", ".png", ".ome.tif", ".ome.tiff", ".svs"]
        File[] patchFiles = patchDirectory.listFiles((File dir, String name) -> validExtensions.any { ext -> name.toLowerCase().endsWith(ext) })

        // String prefix
        // if (patchFiles.length != 0){
        //     prefix = patchFiles[0].getName().replaceAll("_\\[x-.*", "")
        // } else { break }

        String prefix = getPatchPrefix(patchFiles)
        double[] pixelMetadata = getPixelMetadata(patchFiles[0].getName())

        def builder = new SparseImageServer.Builder()

        patchFiles.toList().parallelStream().forEach { patchFile ->

            String patchFilePath = patchFile.getAbsolutePath()
            String patchFileName = patchFile.getName()

            ImageServer<BufferedImage> server = ImageServerProvider.buildServer(patchFilePath, BufferedImage.class)
            RegionRequest request = RegionRequest.createInstance(server)
            BufferedImage img = server.readRegion(request)

            int xPos = patchFileName.replaceAll(/.*x-(\d+),y-\d+.*/, '$1').toInteger()
            int yPos = patchFileName.replaceAll(/.*y-(\d+).*/, '$1').toInteger()
            int height = img.getHeight()
            int width = img.getWidth()
            def region = ImageRegion.createInstance(xPos, yPos, width, height, 0, 0)
            def serverBuilder = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, patchFile.toURI().toString()).getBuilders().get(0)
            builder.jsonRegion(region, 1.0, serverBuilder)

        }

        def server = builder.build()
        server = ImageServers.pyramidalize(server)
        setPixelSizeMicrons(new ImageData(server), pixelMetadata[0], pixelMetadata[1], pixelMetadata[2])

        // Generate output file path
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date())
        String outputFileName = prefix + "_stitched_${timestamp}.ome.tiff"
        String outputPath = new File(outputDir, outputFileName).getAbsolutePath()

        new OMEPyramidWriter.Builder(server)
                .downsamples(server.getPreferredDownsamples())
                .tileSize(512)
                .channelsInterleaved()
                .parallelize()
                .losslessCompression()
                .build()
                .writePyramid(outputPath)

        server.close()

        println("Successfully stitched patches in ${patchDirectory.getName()}!")

    } catch (Exception e){
        logger.error("Processing failed for \"${patchDirectory.getName()}\": ${e.getMessage()}\n")
    }

}

println("Done")

static String getPatchPrefix(File[] patchFiles) throws RuntimeException {

    if (patchFiles.length == 0){
        throw new IllegalArgumentException("Directory is empty")
    }

    String prefix = patchFiles[0].getName().replaceAll("_\\[x-.*", "")
    for (int i = 1; i < patchFiles.length; i++) {
        if (!prefix.equals(patchFiles[i].getName().replaceAll("_\\[x-.*", ""))){
            throw new IllegalStateException("Patch prefixes are inconsistent")
        }
    }

    return prefix
}

static double[] getPixelMetadata(String patchFileName) {

    double pixelHeight = patchFileName.replaceAll(/^.*pHeight-([\d.]+).*$/, '$1').toDouble()
    double pixelWidth = patchFileName.replaceAll(/^.*pWidth-([\d.]+).*$/, '$1').toDouble()
    double zSpacing = patchFileName.replaceAll(/^.*zSp-([\d.]+).*$/, '$1').toDouble()
    double[] pixelMetadata = [pixelHeight, pixelWidth, zSpacing]

    return pixelMetadata
}
