package dev.gaphunter.rabbitmqchannelreusecompanion.gutter

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import dev.gaphunter.rabbitmqchannelreusecompanion.detect.JavaChannelPerPublishFinder
import dev.gaphunter.rabbitmqchannelreusecompanion.detect.KotlinChannelPerPublishFinder
import dev.gaphunter.rabbitmqchannelreusecompanion.model.ChannelPerPublishHit
import dev.gaphunter.rabbitmqchannelreusecompanion.review.ReviewPrompt

class ChannelPerPublishLineMarkerProvider : LineMarkerProviderDescriptor(), DumbAware {

    override fun getName(): String = "RabbitMQ channel created per publish"

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun collectSlowLineMarkers(elements: MutableList<out PsiElement>, result: MutableCollection<in LineMarkerInfo<*>>) {
        val file = elements.firstOrNull()?.containingFile ?: return
        val hits = when (file.language.id) {
            "JAVA" -> JavaChannelPerPublishFinder.findAll(file)
            "kotlin" -> KotlinChannelPerPublishFinder.findAll(file)
            else -> emptyList()
        }
        if (hits.isEmpty()) return

        val hitsByElement = hits.associateBy { it.callElement }
        for (element in elements) {
            val hit = hitsByElement[element] ?: continue
            result.add(buildMarker(hit))

            val path = file.virtualFile?.path ?: continue
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "$path:$lineNumber")
        }
    }

    private fun buildMarker(hit: ChannelPerPublishHit): LineMarkerInfo<PsiElement> {
        val tooltip = "A new Channel is created here and used for basicPublish() in the same method -- " +
            "RabbitMQ's own Java Client API Guide calls this a classic anti-pattern: opening a channel per " +
            "published message is a network round-trip, extremely inefficient compared to a long-lived channel"
        return LineMarkerInfo(
            hit.callElement,
            hit.callElement.textRange,
            ChannelReuseIcons.RISK,
            { _: PsiElement -> tooltip },
            null,
            GutterIconRenderer.Alignment.RIGHT,
            { tooltip },
        )
    }
}
