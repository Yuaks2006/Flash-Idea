package com.flashidea.app.ui.aichat

import org.junit.Assert.assertEquals
import org.junit.Test

class AirMascotStateTest {

    @Test
    fun loadingConversationShowsThinkingState() {
        assertEquals(
            AirMascotState.Thinking,
            resolveAirMascotState(
                isLoading = true,
                messageCount = 1,
                receivingMessageCount = null,
                pipelineMessageCount = null,
                lastCompletedMessageCount = null
            )
        )
    }

    @Test
    fun freshlySubmittedConversationShowsReceiveStateBeforeThinking() {
        assertEquals(
            AirMascotState.Receive,
            resolveAirMascotState(
                isLoading = true,
                messageCount = 1,
                receivingMessageCount = 1,
                pipelineMessageCount = null,
                lastCompletedMessageCount = null
            )
        )
    }

    @Test
    fun stagedWorkShowsPipelineStateAfterInitialThinking() {
        assertEquals(
            AirMascotState.Pipeline,
            resolveAirMascotState(
                isLoading = true,
                messageCount = 1,
                receivingMessageCount = null,
                pipelineMessageCount = 1,
                lastCompletedMessageCount = null
            )
        )
    }

    @Test
    fun newlyCompletedConversationShowsDoneState() {
        assertEquals(
            AirMascotState.Done,
            resolveAirMascotState(
                isLoading = false,
                messageCount = 2,
                receivingMessageCount = null,
                pipelineMessageCount = null,
                lastCompletedMessageCount = 2
            )
        )
    }

    @Test
    fun quietConversationShowsIdleState() {
        assertEquals(
            AirMascotState.Idle,
            resolveAirMascotState(
                isLoading = false,
                messageCount = 2,
                receivingMessageCount = null,
                pipelineMessageCount = null,
                lastCompletedMessageCount = null
            )
        )
    }

    @Test
    fun pipelineStageCyclesAcrossFourProductMoments() {
        assertEquals(AirPipelineStage.LinkingNotes, resolveAirPipelineStage(0))
        assertEquals(AirPipelineStage.Tagging, resolveAirPipelineStage(1))
        assertEquals(AirPipelineStage.Incubating, resolveAirPipelineStage(2))
        assertEquals(AirPipelineStage.Extending, resolveAirPipelineStage(3))
        assertEquals(AirPipelineStage.LinkingNotes, resolveAirPipelineStage(4))
    }
}
