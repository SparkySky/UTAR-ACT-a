package com.meow.utaract;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AiService {
    private static final String TAG = "AiService";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient = new OkHttpClient();
    private final String geminiApiKey;

    public interface AiCallback {
        void onSuccess(String text);
        void onError(Exception e);
    }

    public AiService(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public void summarizeText(String text, AiCallback callback) {
        String prompt = "Summarize the following event description into 4-6 bullet points with key details (what, who, when, where, price, requirements). Be concise and neutral.\n\n" + text;
        generateContent(prompt, callback);
    }

    public void chat(String systemPrompt, String userMessage, AiCallback callback) {
        String prompt = systemPrompt + "\n\nUser: " + userMessage;
        generateContent(prompt, callback);
    }

    private void generateContent(String prompt, AiCallback callback) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        try {
            JSONObject textPart = new JSONObject().put("text", prompt);
            JSONArray parts = new JSONArray().put(textPart);
            JSONObject content = new JSONObject().put("parts", parts);
            JSONArray contents = new JSONArray().put(content);
            JSONObject body = new JSONObject().put("contents", contents);

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(body.toString(), JSON))
                    .build();

            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onError(e);
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (!response.isSuccessful()) {
                            callback.onError(new IOException("Unexpected code " + response));
                            return;
                        }
                        String resp = response.body() != null ? response.body().string() : "";
                        String text = parseGeminiText(resp);
                        callback.onSuccess(text);
                    } catch (Exception e) {
                        callback.onError(e);
                    } finally {
                        if (response.body() != null) {
                            response.close();
                        }
                    }
                }
            });
        } catch (JSONException e) {
            callback.onError(e);
        }
    }

    @Nullable
    private String parseGeminiText(String json) {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray candidates = root.optJSONArray("candidates");
            if (candidates == null || candidates.length() == 0) return "";
            JSONObject candidate0 = candidates.getJSONObject(0);
            JSONObject content = candidate0.optJSONObject("content");
            if (content == null) return "";
            JSONArray parts = content.optJSONArray("parts");
            if (parts == null || parts.length() == 0) return "";
            return parts.getJSONObject(0).optString("text", "");
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse Gemini response", e);
            return "";
        }
    }
}



