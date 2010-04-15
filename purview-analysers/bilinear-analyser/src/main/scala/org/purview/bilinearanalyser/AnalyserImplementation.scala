package org.purview.bilinearanalyser

import org.purview.core.analysis.HeatMapImageAnalyser
import org.purview.core.process.Computation
import org.purview.core.report.Warning
import scala.math._

class AnalyserImplementation extends HeatMapImageAnalyser {
  val name = "Bilinear analyser"
  val description = "Finds bilinearly interpolated regions in an image"
  override val version = Some("1.3")
  override val author = Some("Moritz Roth & David Flemström")

  override val iconResource = Some("icons/analysers/bilinear.png")

  override val message = "Bilinearly scaled region"
  override val reportLevel = Warning

  val markHorizBilinear = for(matrix <- input) yield {
    status("Performing a vertical amplitude scan")

    @inline def between(x: Int, low: Int, high: Int) = if(x < low) low else if(x > high) high else x

    for((x, y, color) <- matrix.cells) yield
      if(y < matrix.height - 1)
        matrix(x, y + 1).weight - color.weight
      else
        0f
  }

  val markVertBilinear = for(matrix <- input) yield {
    status("Performing a vertical amplitude scan")
    for((x, y, color) <- matrix.cells) yield
      if(y < matrix.height - 1)
        matrix(x, y + 1).weight - color.weight
    else
      0f
  }

  private val gaussian5BlurKernel = Array[Float](0.0080f, 0.016f, 0.024f, 0.032f, 0.04f, 0.032f,  0.024f, 0.016f, 0.0080f)

  override val convolve: Computation[Option[Array[Float]]] = Computation(Some(gaussian5BlurKernel))

  val heatmap = markHorizBilinear
}
