package org.purview.errorlevelanalyser

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import org.purview.core.analysis.HeatMapImageAnalyser
import org.purview.core.analysis.Metadata
import org.purview.core.analysis.Settings
import org.purview.core.analysis.settings.FloatRangeSetting
import org.purview.core.report._
import org.purview.core.transforms.ImageToMatrix
import org.purview.core.transforms.MatrixToImage
import scala.math._

class AnalyserImplementation extends HeatMapImageAnalyser with Settings with Metadata {
  val name = "Error level analyser"
  val description = "Uses compression errors in JPEG images to find errors"

  val qualitySetting = new FloatRangeSetting("Quality level", 0, 1, 100)
  qualitySetting.value = 0.2f //default
  val thresholdSetting = new FloatRangeSetting("Threshold", 0, 1, 100)
  thresholdSetting.value = 0.1f //default

  private def quality: Float = qualitySetting.value
  private def artifactThreshold: Float = thresholdSetting.value

  val settings = List(qualitySetting, thresholdSetting)

  override val message = "Noticeable compression artifact"
  override val reportLevel = Warning
  override val threshold = 0f //We implement our own threshold and don't want upstream interference
  override val maxDeviationTolerance = 0.5f

  /** Recompress the given image with a built-in JPEG codec */
  private def introduceJPEGArtifacts(in: BufferedImage) = {
    status("Generating JPEG artifacts")
    val writers = ImageIO.getImageWritersByFormatName("jpeg")

    val out = new ByteArrayOutputStream
    var result: Option[BufferedImage] = None

    while(result.isEmpty && writers.hasNext) {
      val writer = writers.next()
      try {
        val rgb = new BufferedImage(in.getWidth, in.getHeight, BufferedImage.TYPE_INT_RGB)
        val g = rgb.createGraphics
        try {
          g.drawImage(in, 0, 0, null)
        } finally {
          g.dispose()
        }

        writer.setOutput(ImageIO.createImageOutputStream(out))

        val param = writer.getDefaultWriteParam
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT)
        param.setCompressionQuality(quality)

        writer.write(null, new IIOImage(rgb, null, null), param)

        val destroyedImg = ImageIO.read(new ByteArrayInputStream(out.toByteArray))

        val argb = new BufferedImage(in.getWidth, in.getHeight, BufferedImage.TYPE_INT_ARGB)
        val g2 = argb.createGraphics
        try {
          g2.drawImage(destroyedImg, 0, 0, null)
        } finally {
          g2.dispose()
        }

        result = Some(argb)
      } catch {
        case _ => //ignore
      } finally {
        writer.dispose()
        out.close()
      }
    }

    result getOrElse {
      status("Couldn't find any working JPEG writers")
      in
    }
  }

  private val gaussian30Kernel =
    (for(i <- -30 to 30) yield (30 - abs(i)) / (30f * 30f * 30f)).toArray

  override val convolve = Some(gaussian30Kernel)

  private def errorMatrix = input >- MatrixToImage() >- introduceJPEGArtifacts >- ImageToMatrix()

  def diff = for(in <- input; artifacts <- errorMatrix) yield
    in.zip(artifacts).map(x => {
        val w = (x._1 - x._2).weight / 2 //2 because of sqrt(4); we want to average the color
        if(w > artifactThreshold) w else 0
      })

  //Extreme blur!
  def heatmap = diff
}