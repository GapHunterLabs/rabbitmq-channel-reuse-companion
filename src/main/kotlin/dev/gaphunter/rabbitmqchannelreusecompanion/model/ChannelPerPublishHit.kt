package dev.gaphunter.rabbitmqchannelreusecompanion.model

import com.intellij.psi.PsiElement

/** One `createChannel()` call whose result is used for `basicPublish(...)` in the same method body, as a local. */
data class ChannelPerPublishHit(val callElement: PsiElement)
