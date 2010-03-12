package org.purview.core.session

import org.purview.core.analysis.Analyser
import org.purview.core.analysis.Metadata
import org.purview.core.report.ReportEntry

class AnalysisSession[A](analysersToRun: Seq[Analyser[A]], input: A) {
  def run(implicit stats: AnalysisStats): Map[Analyser[A], Set[ReportEntry]] = {
    val progScale = 1f / analysersToRun.length
    val results = for {
      i <- 0 until analysersToRun.length
      analyser = analysersToRun(i)
    } yield {
      stats.reportProgress(i * progScale)
      stats.reportSubProgress(0)
      analyser match {
        case m: Metadata =>
          stats.reportAnalyser(m.name)
        case _ => //No metadata available
          stats.reportAnalyser("Unknown")
      }
      val res = analyser.analyseWithStats(input)
      res
    }
    
    stats.reportProgress(1)
    stats.reportSubProgress(1)
    
    Map((analysersToRun zip results): _*)
  }
}
