package dev.gaphunter.rabbitmqchannelreusecompanion.detect

import com.intellij.psi.JavaRecursiveElementWalkingVisitor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLocalVariable
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.util.PsiTreeUtil
import dev.gaphunter.rabbitmqchannelreusecompanion.model.ChannelPerPublishHit

/**
 * Finds a local variable initialized with `....createChannel()`
 * (RabbitMQ Java client) whose declaring method body also calls
 * `.basicPublish(...)` on that same variable -- RabbitMQ's own Java
 * Client API Guide calls this a "classic anti-pattern to be avoided":
 * "opening a channel for each published message... Channels are
 * supposed to be reasonably long-lived and opening a new one is a
 * network round-trip which makes this pattern extremely inefficient."
 *
 * **v0.1 scope, stated honestly:** matches by simple method/variable
 * name, not real type resolution -- an unrelated `createChannel()`
 * method on some other type is a possible (rare) false positive. Only
 * a `createChannel()` result assigned to a *local* variable and used
 * for `basicPublish` in the *same* method is flagged -- a channel
 * stored as an instance field (the correct long-lived pattern) is
 * never flagged.
 */
object JavaChannelPerPublishFinder {

    fun findAll(file: PsiFile): List<ChannelPerPublishHit> {
        val hits = mutableListOf<ChannelPerPublishHit>()
        file.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitLocalVariable(variable: PsiLocalVariable) {
                super.visitLocalVariable(variable)
                hitFor(variable)?.let { hits += it }
            }
        })
        return hits
    }

    private fun hitFor(variable: PsiLocalVariable): ChannelPerPublishHit? {
        val initializer = variable.initializer as? PsiMethodCallExpression ?: return null
        if (initializer.methodExpression.referenceName != "createChannel") return null

        val varName = variable.name
        val containingMethod = PsiTreeUtil.getParentOfType(variable, PsiMethod::class.java) ?: return null
        val methodBody = containingMethod.body ?: return null

        var publishesOnVariable = false
        methodBody.accept(object : JavaRecursiveElementWalkingVisitor() {
            override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                super.visitMethodCallExpression(expression)
                if (expression.methodExpression.referenceName != "basicPublish") return
                val qualifier = expression.methodExpression.qualifierExpression ?: return
                if (qualifier.text == varName) publishesOnVariable = true
            }
        })
        if (!publishesOnVariable) return null

        return ChannelPerPublishHit(leafOf(initializer))
    }

    /** Descends to a real leaf PSI element -- LineMarkerInfo must never anchor on a composite node. */
    private fun leafOf(element: PsiElement): PsiElement {
        var current = element
        while (current.firstChild != null) current = current.firstChild
        return current
    }
}
