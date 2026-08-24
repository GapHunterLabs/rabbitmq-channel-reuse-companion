package dev.gaphunter.rabbitmqchannelreusecompanion.detect

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.rabbitmqchannelreusecompanion.model.ChannelPerPublishHit
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

/** Kotlin counterpart of [JavaChannelPerPublishFinder]. */
object KotlinChannelPerPublishFinder {

    fun findAll(file: PsiFile): List<ChannelPerPublishHit> {
        if (file !is KtFile) return emptyList()
        val hits = mutableListOf<ChannelPerPublishHit>()
        file.accept(object : KtTreeVisitorVoid() {
            override fun visitProperty(property: KtProperty) {
                super.visitProperty(property)
                if (property.isLocal) hitFor(property)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(property: KtProperty): ChannelPerPublishHit? {
        val initializer = property.initializer as? KtDotQualifiedExpression ?: return null
        val initCall = initializer.selectorExpression as? KtCallExpression ?: return null
        if (initCall.calleeExpression?.text != "createChannel") return null

        val varName = property.name ?: return null
        val containingFunction = PsiTreeUtil.getParentOfType(property, KtNamedFunction::class.java) ?: return null
        val functionBody = containingFunction.bodyExpression ?: return null

        var publishesOnVariable = false
        functionBody.accept(object : KtTreeVisitorVoid() {
            override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
                super.visitDotQualifiedExpression(expression)
                val call = expression.selectorExpression as? KtCallExpression ?: return
                if (call.calleeExpression?.text != "basicPublish") return
                if (expression.receiverExpression.text == varName) publishesOnVariable = true
            }
        })
        if (!publishesOnVariable) return null

        return ChannelPerPublishHit(leafOf(initCall))
    }

    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
