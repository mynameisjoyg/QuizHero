package com.joyg.quizhero

import android.util.Log
import com.google.gson.Gson
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import io.reactivex.disposables.CompositeDisposable

class QuizStompClient {

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()

    // 模擬器連線電腦 localhost 請用 10.0.2.2，若用實體手機測試請改為電腦的區域 IP (如 192.168.x.x)
    private val wsUrl = "ws://10.77.80.205:8080/ws-quiz/websocket"

    fun connect(
        playerId: String,
        onConnected: () -> Unit,
        onMatched: (MatchResponse) -> Unit,
        onQuizReceived: (QuizQuestion) -> Unit,
        onResultReceived: (BattleResult) -> Unit
    ) {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

        // 1. 監聽 STOMP 生命週期
        val lifecycleSub = stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d("STOMP", "✅ WebSocket 連線建立成功")
                    onConnected()

                    // 連線成功後，立刻訂閱玩家個人配對頻道
                    subscribeMatchChannel(playerId, onMatched, onQuizReceived, onResultReceived)
                }
                LifecycleEvent.Type.ERROR -> Log.e("STOMP", "❌ 連線錯誤", event.exception)
                LifecycleEvent.Type.CLOSED -> Log.d("STOMP", "🔒 連線已關閉")
                else -> {}
            }
        }
        lifecycleSub?.let { compositeDisposable.add(it) }

        stompClient?.connect()
    }

    // 2. 訂閱個人配對頻道 (/topic/room/matched/{playerId})
    private fun subscribeMatchChannel(
        playerId: String,
        onMatched: (MatchResponse) -> Unit,
        onQuizReceived: (QuizQuestion) -> Unit,
        onResultReceived: (BattleResult) -> Unit
    ) {
        //val topic = "/topic/room/matched/$playerId"
        val topic = "/topic/matchmaking"
        val sub = stompClient?.topic(topic)?.subscribe { stompMessage ->
            val matchResponse = gson.fromJson(stompMessage.payload, MatchResponse::class.java)
            Log.d("STOMP", "🎉 配對成功！ 房間 ID: ${matchResponse.roomId}")

            onMatched(matchResponse)

            // 拿到 roomId 後，立刻訂閱房間的題目與搶答結果頻道
            subscribeRoomChannels(matchResponse.roomId, onQuizReceived, onResultReceived)
        }
        sub?.let { compositeDisposable.add(it) }
    }

    // 3. 訂閱房間專屬頻道 (/quiz 與 /result)
    private fun subscribeRoomChannels(
        roomId: String,
        onQuizReceived: (QuizQuestion) -> Unit,
        onResultReceived: (BattleResult) -> Unit
    ) {
        // A. 訂閱題目
        val quizSub = stompClient?.topic("/topic/room/$roomId/quiz")?.subscribe { message ->
            val question = gson.fromJson(message.payload, QuizQuestion::class.java)
            onQuizReceived(question)
        }

        // B. 訂閱搶答結果
        val resultSub = stompClient?.topic("/topic/room/$roomId/result")?.subscribe { message ->
            val result = gson.fromJson(message.payload, BattleResult::class.java)
            onResultReceived(result)
        }

        quizSub?.let { compositeDisposable.add(it) }
        resultSub?.let { compositeDisposable.add(it) }
    }

    // 4. 發送配對請求 (/app/matchmaking)
    fun sendMatchRequest(playerId: String) {
        val payload = gson.toJson(MatchRequest(playerId))
        stompClient?.send("/app/matchmaking", payload)?.subscribe({
            Log.d("STOMP", "📤 已送出配對請求: $playerId")
        }, { t ->
            Log.e("STOMP", "配對請求發送失敗", t)
        })?.let { compositeDisposable.add(it) }
    }

    // 5. 發送搶答答案 (/app/room/{roomId}/answer)
    fun sendAnswer(roomId: String, questionId: String, selectedOption: String) {
        val payload = gson.toJson(AnswerPayload(questionId, selectedOption))
        stompClient?.send("/app/room/$roomId/answer", payload)?.subscribe({
            Log.d("STOMP", "⚡ 已送出搶答: $selectedOption")
        }, { t ->
            Log.e("STOMP", "搶答發送失敗", t)
        })?.let { compositeDisposable.add(it) }
    }

    // 斷開連線與釋放資源
    fun disconnect() {
        compositeDisposable.clear()
        stompClient?.disconnect()
    }
}