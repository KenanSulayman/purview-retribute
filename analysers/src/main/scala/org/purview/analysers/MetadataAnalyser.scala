package org.purview.analysers

import org.purview.core.analysis.Analyser
import org.purview.core.analysis.Metadata
import org.purview.core.data.ImageMatrix
import org.purview.core.report.Error
import org.purview.core.report.Message
import org.purview.core.report.ReportEntry
import org.purview.core.report.Warning

class MetadataAnalyser extends Analyser[ImageMatrix] with Metadata {
  val name = "Metadata analyser"
  val description = "Detects suspicious metadata entries in the file of an image"

  type MetaAnalyser = PartialFunction[(String, String, String), (Boolean, String)]

  private val metadataTesters: Set[MetaAnalyser] =
    Set(photoshopDetector, paintDotNetDetector, gimpDetector)

  def result = for(image <- input) yield {
    (for {
        tester <- metadataTesters
        tupled = tester.lift
        (dir, tree) <- image.metadata
        (key, value) <- tree
        (critical, msg) <- tupled((dir, key, value))
      } yield new ReportEntry with Message {
        val level = if(critical) Error else Warning
        val message = msg
      }).toSet
  }

  def photoshopDetector: MetaAnalyser = {
    case ("Exif", "Software", software) if software contains "Photoshop" =>
      (true, "The image was edited by Adobe® Photoshop")
    case ("Xmp", event, software) if (event contains "softwareAgent") && (software contains "Photoshop") =>
      //XMP isn't actually suported by default; this is just to demonstrate...
      (true, "The image contains history generated by Adobe® Photoshop")
  }

  def paintDotNetDetector: MetaAnalyser = {
    case ("Exif", "Software", software) if software.toLowerCase contains "paint.net" =>
      (true, "The image was edited by Paint.NET")
  }

  def gimpDetector: MetaAnalyser = {
    case ("Exif", "Software", software) if software.toLowerCase contains "gimp" =>
      (true, "The image was edited by GIMP")
  }
}
