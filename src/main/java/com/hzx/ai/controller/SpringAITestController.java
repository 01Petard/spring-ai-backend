package com.hzx.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiAudioSpeechModel;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.openai.audio.speech.SpeechPrompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/test")
public class SpringAITestController {

    /**
     * 本地优先：Ollama ChatModel（由 spring-ai-ollama-spring-boot-starter 自动装配）
     */
    private final OllamaChatModel ollamaChatModel;

    /**
     * 简单对话
     */
    @GetMapping("/simple")
    public Map<String, String> completion(
            @RequestParam(value = "message", defaultValue = "给我讲个笑话") String message
    ) {
        // 用 ChatClient 包一层，后端具体用哪个模型由注入的 ChatModel 决定（此处是 Ollama）
        String value = ChatClient.create(ollamaChatModel)
                .prompt().user(message).call().content();

        return Map.of("generation", Objects.requireNonNull(value));
    }

    /**
     * 服务端推送流（SSE）
     */
//    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @GetMapping(value = "/stream", produces = "text/html;charset=utf-8")
//    @GetMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> stream(
            @RequestParam(value = "message", defaultValue = "给我讲个笑话") String message) {

        return ChatClient.create(ollamaChatModel)
                .prompt().user(message)
                .stream()
                .content();
    }


    /**
     * 多模态：文字 + 图片（本地 Ollama 视觉模型 qwen2.5vl:3b）
     */
    @GetMapping("/ollama/multimodal")
    public String multimodal(
            @RequestParam(value = "message", defaultValue = "这个图片你看出什么?大胆猜测一下这个人的身份") String message
    ) throws IOException {
        // 图片路径
        Media imageMedia = Media.builder()

                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new ClassPathResource("知识库/jack.png"))

                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                .data(new ClassPathResource("知识库/elf.jpg"))

                .build();

        // 创建用户消息
        UserMessage userMessage = new UserMessage(message, List.of(imageMedia));

        // 调用本地 Ollama 视觉模型
        ChatResponse response = ollamaChatModel.call(new Prompt(userMessage));

        return response.getResult().getOutput().getText();
    }

    private final ChatClient chatClient;


    /**
     * 多模态：文字 + 图片（Ollama 视觉模型：llama3.2-vision 或 qwen2.5-vl）
     */
    @GetMapping("/openai/multimodal")
    public String multimodal_OpenAI(
            @RequestParam(value = "message", defaultValue = "这个图片你看出什么?大胆猜测一下这个人的身份") String message
    ) {
        Media imageMedia = Media.builder()

                .mimeType(MimeTypeUtils.IMAGE_PNG)
                .data(new ClassPathResource("知识库/jack.png"))

                .mimeType(MimeTypeUtils.IMAGE_JPEG)
                .data(new ClassPathResource("知识库/elf.jpg"))

                .build();

        return chatClient
                .prompt()
                .user(u -> u.text(message).media(imageMedia))
                .call()
                .content();
    }


    /* -----------------------
     * 下面两段：语音转文本 / 文本转语音
     * 说明：Spring AI 官方目前仅对 OpenAI 提供内置 STT/TTS 模型支持。
     * 如果你要走阿里百炼或纯本地，请看文后“可选方案”。
     * ----------------------- */

    // ========== OpenAI 语音转文本（Whisper）正确写法 ==========
    // 需要引入：spring-ai-openai-spring-boot-starter
    @Autowired(required = false)
    private OpenAiAudioTranscriptionModel openAiTranscriptionModel;

    @GetMapping("/audio2text")
    public String audio2text() throws IOException {
        if (openAiTranscriptionModel == null) {
            return "未启用 OpenAI STT（当前未加载 OpenAI 转写模型 Bean）。";
        }

        var audioFile = new ClassPathResource("/hello.mp3");

        var options = OpenAiAudioTranscriptionOptions.builder()
                // 注意：是 responseFormat(...)，不是 withResponseFormat(...)
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .temperature(0f)
                .build();

        var prompt = new AudioTranscriptionPrompt(audioFile, options);
        var resp = openAiTranscriptionModel.call(prompt);
        return resp.getResult().getOutput();
    }

    // ========== OpenAI 文本转语音（TTS）正确写法（推荐用模型封装而不是裸 API） ==========
    @Autowired(required = false)
    private OpenAiAudioSpeechModel openAiSpeechModel;

    @GetMapping("/text2audio")
    public String text2audio(
            @RequestParam(defaultValue = "你好，我是徐庶") String text
    ) throws IOException {
        if (openAiSpeechModel == null) {
            return "未启用 OpenAI TTS（当前未加载 OpenAI 语音合成模型 Bean）。";
        }

        var options = OpenAiAudioSpeechOptions.builder()
                // 注意：是 voice(...) / responseFormat(...) / model(...)，不是 withVoice/withInput
                .voice(OpenAiAudioApi.SpeechRequest.Voice.ONYX)
                .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3)
                .model(OpenAiAudioApi.TtsModel.TTS_1.value)
                .build();

        var prompt = new SpeechPrompt(text, options);
        var response = openAiSpeechModel.call(prompt);

        byte[] bytes = response.getResult().getOutput();
        java.nio.file.Files.write(java.nio.file.Path.of(System.getProperty("user.dir"), "xushu.mp3"), bytes);

        return "ok";
    }
}
