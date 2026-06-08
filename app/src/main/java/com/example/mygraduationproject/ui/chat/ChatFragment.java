package com.example.mygraduationproject.ui.chat;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mygraduationproject.R;
import com.example.mygraduationproject.config.ApiConfig;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.databinding.FragmentChatBinding;
import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.ChatMessageEntity;
import com.example.mygraduationproject.model.ChatSession;
import com.example.mygraduationproject.model.PlantImage;
import com.example.mygraduationproject.network.ApiService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** 灏忔鍔╂墜锛欰I妞嶇墿璇嗗埆瀵硅瘽銆佸悕绉扮籂姝ｃ€佸巻鍙蹭細璇濄€佸浘鐗囬€夋嫨 */
public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    
    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private ApiService apiService;
    private Repository repository;
    private ChatViewModel viewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getContext() != null) {
            apiService = new ApiService(getContext());
            repository = Repository.getInstance(getContext());
        }
        
        adapter = new ChatAdapter();
        binding.rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMessages.setAdapter(adapter);
        
        binding.btnSend.setOnClickListener(v -> sendMessage());
        binding.btnAttachImage.setOnClickListener(v -> showImagePickerDialog());
        binding.btnHistory.setOnClickListener(v -> showSessionsDialog());
        
        restoreState();
    }

    private void restoreState() {
        ChatSession savedSession = viewModel.getCurrentSession();
        AIResult savedResult = viewModel.getCurrentAIResult();
        
        if (savedSession != null) {
            loadSession(savedSession, savedResult);
        } else {
            addWelcomeMessage();
        }
    }

    private void addWelcomeMessage() {
        ApiService.ChatMessage welcomeMessage = new ApiService.ChatMessage(
                ApiConfig.AI_GREETING, false);
        adapter.addMessage(welcomeMessage);
    }

    private void showSessionsDialog() {
        if (repository == null || getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_chat_sessions, null, false);
        RecyclerView rvSessions = dialogView.findViewById(R.id.rvSessions);
        TextView tvNoSessions = dialogView.findViewById(R.id.tvNoSessions);
        
        rvSessions.setLayoutManager(new LinearLayoutManager(getContext()));
        ChatSessionAdapter sessionAdapter = new ChatSessionAdapter();
        rvSessions.setAdapter(sessionAdapter);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setNegativeButton("关闭", null)
                .create();
        
        sessionAdapter.setOnSessionClickListener(session -> {
            dialog.dismiss();
            loadSession(session, null);
        });
        
        repository.getAllChatSessions(sessions -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (sessions != null && !sessions.isEmpty()) {
                        sessionAdapter.setSessions(sessions);
                        if (viewModel.getCurrentSession() != null) {
                            sessionAdapter.setCurrentSessionId(viewModel.getCurrentSession().getId());
                        }
                        tvNoSessions.setVisibility(View.GONE);
                        rvSessions.setVisibility(View.VISIBLE);
                    } else {
                        tvNoSessions.setVisibility(View.VISIBLE);
                        rvSessions.setVisibility(View.GONE);
                    }
                });
            }
        });
        
        dialog.show();
    }

    private void loadSession(ChatSession session, AIResult preloadedResult) {
        if (session == null || repository == null) return;
        
        viewModel.setCurrentSession(session);
        currentSession = session;
        binding.tvToolbarTitle.setText(session.getPlantName() != null ? session.getPlantName() : "小植");
        
        adapter.clear();
        
        repository.getMessagesBySession(session.getId(), messages -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (messages != null && !messages.isEmpty()) {
                        for (ChatMessageEntity msg : messages) {
                            adapter.addMessage(new ApiService.ChatMessage(msg.getContent(), msg.isUser()));
                        }
                        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    } else {
                        addWelcomeMessage();
                    }
                });
            }
        });
        
        if (preloadedResult != null) {
            viewModel.setCurrentAIResult(preloadedResult);
            currentAIResult = preloadedResult;
        } else if (session.getImagePath() != null) {
            repository.getAIResultByImagePath(session.getImagePath(), result -> {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        viewModel.setCurrentAIResult(result);
                        currentAIResult = result;
                    });
                }
            });
        }
    }

    private ChatSession currentSession;
    private AIResult currentAIResult;

    private void showImagePickerDialog() {
        if (repository == null || getContext() == null) return;
        
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_image_picker, null, false);
        RecyclerView rvImages = dialogView.findViewById(R.id.rvImages);
        TextView tvNoImages = dialogView.findViewById(R.id.tvNoImages);
        
        rvImages.setLayoutManager(new GridLayoutManager(getContext(), 3));
        ImagePickerAdapter imageAdapter = new ImagePickerAdapter();
        rvImages.setAdapter(imageAdapter);
        
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("选择图片")
                .setView(dialogView)
                .setNegativeButton("取消", null)
                .create();
        
        imageAdapter.setOnImageSelectedListener(image -> {
            dialog.dismiss();
            if (image != null) {
                handleSelectedImage(image);
            }
        });
        
        repository.getRecentImages(50, images -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (images != null && !images.isEmpty()) {
                        imageAdapter.setImages(images);
                        tvNoImages.setVisibility(View.GONE);
                        rvImages.setVisibility(View.VISIBLE);
                    } else {
                        loadAIResultsAsImages(imageAdapter, tvNoImages, rvImages);
                    }
                });
            }
        });
        
        dialog.show();
    }

    private void loadAIResultsAsImages(ImagePickerAdapter imageAdapter, TextView tvNoImages, RecyclerView rvImages) {
        repository.getRecentAIResults(50, results -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    if (results != null && !results.isEmpty()) {
                        List<PlantImage> plantImages = new ArrayList<>();
                        for (AIResult result : results) {
                            if (result.getImagePath() != null && !result.getImagePath().isEmpty()) {
                                PlantImage pi = new PlantImage(result.getImagePath(), 0);
                                pi.setId(result.getImageId());
                                plantImages.add(pi);
                            }
                        }
                        if (!plantImages.isEmpty()) {
                            imageAdapter.setImages(plantImages);
                            tvNoImages.setVisibility(View.GONE);
                            rvImages.setVisibility(View.VISIBLE);
                            return;
                        }
                    }
                    tvNoImages.setVisibility(View.VISIBLE);
                    rvImages.setVisibility(View.GONE);
                });
            }
        });
    }

    private void handleSelectedImage(PlantImage image) {
        if (binding == null || image == null) return;
        
        String imagePath = image.getImagePath();
        boolean isUrl = imagePath != null && (imagePath.startsWith("http://") || imagePath.startsWith("https://"));
        
        if (isUrl) {
            repository.getAIResultByImagePath(imagePath, aiResult -> {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (aiResult != null) {
                            repository.getChatSessionByImagePath(imagePath, session -> {
                                if (getActivity() != null && !getActivity().isFinishing()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (session != null) {
                                            loadSession(session, aiResult);
                                        } else {
                                            createSessionFromAIResult(aiResult);
                                        }
                                    });
                                }
                            });
                        } else {
                            Toast.makeText(getContext(), "未找到该图片的识别记录", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        } else {
            repository.getChatSessionByImagePath(imagePath, session -> {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        if (session != null) {
                            loadSession(session, null);
                        } else {
                            repository.getAIResultByImagePath(imagePath, aiResult -> {
                                if (getActivity() != null && !getActivity().isFinishing()) {
                                    getActivity().runOnUiThread(() -> {
                                        if (aiResult != null) {
                                            createSessionFromAIResult(aiResult);
                                        } else {
                                            createNewSessionAndIdentify(image);
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
            });
        }
    }

    private void createSessionFromAIResult(AIResult aiResult) {
        ChatSession newSession = new ChatSession(aiResult.getImagePath(), aiResult.getPlantName());
        repository.insertChatSession(newSession, sessionId -> {
            if (getActivity() != null && !getActivity().isFinishing()) {
                getActivity().runOnUiThread(() -> {
                    newSession.setId(sessionId);
                    currentSession = newSession;
                    viewModel.setCurrentSession(newSession);
                    currentAIResult = aiResult;
                    viewModel.setCurrentAIResult(aiResult);
                    binding.tvToolbarTitle.setText(aiResult.getPlantName() != null ? aiResult.getPlantName() : "小植");

                    adapter.clear();

                    StringBuilder response = new StringBuilder();
                    response.append("我识别了这张图片：\n\n");
                    response.append("🌱 植物名称：").append(aiResult.getPlantName() != null ? aiResult.getPlantName() : "未知").append("\n");
                    if (aiResult.getConfidence() > 0) {
                        response.append("📊 置信度：").append(String.format("%.1f%%", aiResult.getConfidence() * 100)).append("\n");
                    }
                    response.append("💚 健康状态：").append(aiResult.getHealthStatus() != null ? aiResult.getHealthStatus() : "--").append("\n");
                    if (aiResult.getHealthScore() > 0) {
                        response.append("📊 健康评分：").append(String.format("%.1f", aiResult.getHealthScore())).append("\n");
                    }
                    if (aiResult.getDetailedAnalysis() != null && !aiResult.getDetailedAnalysis().isEmpty()) {
                        response.append("\n📝 详细分析：\n").append(aiResult.getDetailedAnalysis()).append("\n\n");
                    }
                    response.append("如果识别有误，请告诉我正确的植物名称，我会重新分析~");

                    saveMessage(sessionId, response.toString(), false);
                    ApiService.ChatMessage infoMessage = new ApiService.ChatMessage(response.toString(), false);
                    adapter.addMessage(infoMessage);
                    binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                });
            }
        });
    }

    private void createNewSessionAndIdentify(PlantImage image) {
        if (image == null || image.getImagePath() == null) return;
        
        File imageFile = new File(image.getImagePath());
        if (!imageFile.exists()) return;
        
        showProgress(true);
        
        apiService.identifyPlant(imageFile, new ApiService.PlantIdentifyCallback() {
            @Override
            public void onSuccess(List<AIResult> results) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        
                        if (results == null || results.isEmpty()) {
                            Toast.makeText(getContext(), "识别失败，请重试", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        AIResult result = results.get(0);
                        currentAIResult = result;
                        viewModel.setCurrentAIResult(result);
                        result.setImagePath(image.getImagePath());
                        
                        ChatSession newSession = new ChatSession(image.getImagePath(), result.getPlantName());
                        
                        repository.insertChatSession(newSession, sessionId -> {
                            if (getActivity() != null && !getActivity().isFinishing()) {
                                getActivity().runOnUiThread(() -> {
                                    newSession.setId(sessionId);
                                    currentSession = newSession;
                                    viewModel.setCurrentSession(newSession);
                                    binding.tvToolbarTitle.setText(result.getPlantName() != null ? result.getPlantName() : "小植");
                                    
                                    adapter.clear();
                                    
                                    StringBuilder response = new StringBuilder();
                                    response.append("我识别了这张图片：\n\n");
                                    response.append("🌱 植物名称：").append(result.getPlantName() != null ? result.getPlantName() : "未知").append("\n");
                                    response.append("📊 置信度：").append(String.format("%.1f%%", result.getConfidence() * 100)).append("\n");
                                    response.append("💚 健康状态：").append(result.getHealthStatus() != null ? result.getHealthStatus() : "--").append("\n\n");
                                    
                                    if (results.size() > 1) {
                                        response.append("其他候选结果：\n");
                                        for (int i = 1; i < results.size(); i++) {
                                            AIResult r = results.get(i);
                                            response.append("• ").append(r.getPlantName())
                                                    .append(" (").append(String.format("%.1f%%", r.getConfidence() * 100)).append(")\n");
                                        }
                                        response.append("\n");
                                    }
                                    
                                    if (result.getDetailedAnalysis() != null && !result.getDetailedAnalysis().isEmpty()) {
                                        response.append("📝 详细分析：\n").append(result.getDetailedAnalysis()).append("\n\n");
                                    }
                                    
                                    response.append("如果识别有误，请告诉我正确的植物名称，我会重新分析~");
                                    
                                    saveMessage(sessionId, response.toString(), false);
                                    
                                    ApiService.ChatMessage infoMessage = new ApiService.ChatMessage(response.toString(), false);
                                    adapter.addMessage(infoMessage);
                                    binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                                    
                                    // 检查图片是否已经存在于数据库中
                                    if (repository != null) {
                                        if (image.getId() > 0) {
                                            // 图片已存在，直接使用现有的 imageId
                                            result.setImageId(image.getId());
                                            repository.insertAIResult(result, id -> {
                                                result.setId(id);
                                                currentAIResult = result;
                                                viewModel.setCurrentAIResult(result);
                                            });
                                        } else {
                                            // 图片不存在，先插入图片记录
                                            repository.insertPlantImage(image, imageId -> {
                                                if (imageId > 0) {
                                                    result.setImageId(imageId);
                                                    repository.insertAIResult(result, id -> {
                                                        result.setId(id);
                                                        currentAIResult = result;
                                                        viewModel.setCurrentAIResult(result);
                                                    });
                                                }
                                            });
                                        }
                                    }
                                });
                            }
                        });
                    });
                }
            }
            
            @Override
            public void onError(String message) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void sendMessage() {
        if (binding == null) return;
        
        String messageText = binding.etMessage.getText().toString().trim();
        if (messageText.isEmpty()) return;
        
        ApiService.ChatMessage userMessage = new ApiService.ChatMessage(messageText, true);
        adapter.addMessage(userMessage);
        binding.etMessage.setText("");
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        
        if (currentSession != null) {
            saveMessage(currentSession.getId(), messageText, true);
            updateSessionLastMessage(currentSession.getId(), messageText);
        }
        
        if (currentAIResult != null && isCorrectionMessage(messageText)) {
            handleCorrection(messageText);
            return;
        }
        
        showProgress(true);
        
        String contextPrompt = buildContextPrompt(messageText);
        List<ApiService.ChatMessage> history = adapter.getMessages();
        
        apiService.chatWithAI(contextPrompt, history, new ApiService.ChatCallback() {
            @Override
            public void onSuccess(String response) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        ApiService.ChatMessage aiMessage = new ApiService.ChatMessage(response, false);
                        adapter.addMessage(aiMessage);
                        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        
                        if (currentSession != null) {
                            saveMessage(currentSession.getId(), response, false);
                            updateSessionLastMessage(currentSession.getId(), response);
                        }
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private boolean isCorrectionMessage(String message) {
        String lowerMsg = message.toLowerCase();
        return lowerMsg.contains("错了") || lowerMsg.contains("不对") || 
               lowerMsg.contains("不是") || lowerMsg.contains("应该是") ||
               lowerMsg.contains("实际上是") || lowerMsg.contains("这是");
    }

    /**
     * 处理植物名称纠正
     * 当用户指出植物识别错误时：
     * 1. 调用 AI 重新分析正确的植物
     * 2. 更新数据库中的原始记录
     * 3. 更新分析界面的历史记录
     * 4. 更新当前会话的植物名称
     */
    private void handleCorrection(String message) {
        if (currentAIResult == null || apiService == null) {
            Log.e("ChatFragment", "handleCorrection: currentAIResult is null");
            return;
        }
        
        // 检查 ID 是否有效
        if (currentAIResult.getId() <= 0) {
            Log.e("ChatFragment", "handleCorrection: currentAIResult ID 无效，ID=" + currentAIResult.getId());
            ApiService.ChatMessage errorMessage = new ApiService.ChatMessage(
                    "数据还在处理中，请稍后再试~", false);
            adapter.addMessage(errorMessage);
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            return;
        }
        
        String correctName = extractPlantName(message);
        if (correctName == null || correctName.isEmpty()) {
            ApiService.ChatMessage askMessage = new ApiService.ChatMessage(
                    "请告诉我正确的植物名称，我会重新为您分析~", false);
            adapter.addMessage(askMessage);
            binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
            return;
        }
        
        showProgress(true);
        
        ApiService.ChatMessage confirmMessage = new ApiService.ChatMessage(
                "好的，我来重新分析【" + correctName + "】...", false);
        adapter.addMessage(confirmMessage);
        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
        
        // 保存原始 ID 和 imageId
        final long originalId = currentAIResult.getId();
        final long originalImageId = currentAIResult.getImageId();
        final String originalImagePath = currentAIResult.getImagePath();
        final long originalTimestamp = currentAIResult.getTimestamp();
        
        Log.i("ChatFragment", "开始纠正植物名称：" + correctName + ", originalId=" + originalId + ", originalImageId=" + originalImageId);
        
        apiService.correctPlantName(currentAIResult, correctName, new ApiService.CorrectCallback() {
            @Override
            public void onSuccess(AIResult result) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        
                        // 恢复原始 ID 和 imageId，确保数据库更新正确
                        result.setId(originalId);
                        result.setImageId(originalImageId);
                        result.setImagePath(originalImagePath);
                        result.setTimestamp(originalTimestamp);
                        
                        // 更新当前 AI 结果
                        currentAIResult = result;
                        viewModel.setCurrentAIResult(result);
                        
                        // 更新数据库中的原始记录（使用安全更新方法，避免外键约束错误）
                        if (repository != null && result != null && originalId > 0) {
                            repository.updatePlantInfoSafely(
                                originalId,
                                correctName,
                                result.getConfidence(),
                                result.getHealthStatus(),
                                result.getDetailedAnalysis(),
                                result.getHealthScore(),
                                result.getWaterTemp(),
                                result.getAirTemp(),
                                result.getAirHumidity(),
                                result.getPlantType(),
                                success -> {
                                    if (success) {
                                        Log.i("ChatFragment", "已更新数据库记录，ID=" + originalId + ", plantName=" + correctName);
                                        
                                        // 通知分析界面刷新数据（通过广播）
                                        if (getContext() != null) {
                                            Intent refreshIntent = new Intent("ACTION_PLANT_DATA_REFRESHED");
                                            refreshIntent.putExtra("ai_result_id", originalId);
                                            refreshIntent.putExtra("plant_name", correctName);
                                            getContext().sendBroadcast(refreshIntent);
                                            Log.i("ChatFragment", "已发送广播刷新通知，plant_name=" + correctName);
                                        }
                                        
                                        if (getActivity() != null && !getActivity().isFinishing()) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "数据库已更新：" + correctName, Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    } else {
                                        Log.e("ChatFragment", "更新数据库失败，ID=" + originalId);
                                        if (getActivity() != null && !getActivity().isFinishing()) {
                                            getActivity().runOnUiThread(() -> {
                                                Toast.makeText(getContext(), "更新数据库失败", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    }
                                }
                            );
                        }
                        
                        // 更新当前会话
                        if (currentSession != null) {
                            currentSession.setPlantName(correctName);
                            currentSession.setLastMessage("已更正为：" + correctName);
                            currentSession.setUpdatedAt(System.currentTimeMillis());
                            repository.updateChatSession(currentSession);
                            viewModel.setCurrentSession(currentSession);
                            binding.tvToolbarTitle.setText(correctName);
                        }
                        
                        // 构建响应消息
                        StringBuilder response = new StringBuilder();
                        response.append("✅ 已更新！这是【").append(correctName).append("】的重新分析结果：\n\n");
                        response.append("🌱 植物名称：").append(result.getPlantName()).append("\n");
                        response.append("📊 置信度：").append(String.format("%.1f%%", result.getConfidence() * 100)).append("\n");
                        response.append("💚 健康状态：").append(result.getHealthStatus() != null ? result.getHealthStatus() : "--").append("\n\n");
                        
                        if (result.getDetailedAnalysis() != null && !result.getDetailedAnalysis().isEmpty()) {
                            response.append("📝 详细分析：\n").append(result.getDetailedAnalysis());
                        }
                        
                        response.append("\n\n📌 已同步更新：\n");
                        response.append("• 分析界面的历史记录\n");
                        response.append("• 本次会话记录\n");
                        response.append("• 原始分析数据");
                        
                        ApiService.ChatMessage resultMessage = new ApiService.ChatMessage(response.toString(), false);
                        adapter.addMessage(resultMessage);
                        binding.rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        
                        // 保存消息到会话记录
                        if (currentSession != null) {
                            saveMessage(currentSession.getId(), response.toString(), false);
                        }
                        
                        Log.i("ChatFragment", "消息已添加到界面，内容长度=" + response.length());
                    });
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                if (getActivity() != null && !getActivity().isFinishing()) {
                    getActivity().runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private String extractPlantName(String message) {
        String[] patterns = {"应该是", "实际上是", "这是", "不是", "错了，是", "不对，是", "叫"};
        for (String pattern : patterns) {
            if (message.contains(pattern)) {
                int index = message.indexOf(pattern);
                String after = message.substring(index + pattern.length()).trim();
                String[] words = after.split("[，。、！？\\s]+");
                if (words.length > 0) {
                    return words[0];
                }
            }
        }
        
        String lowerMsg = message.toLowerCase();
        if (lowerMsg.contains("错了") || lowerMsg.contains("不对") || lowerMsg.contains("不是")) {
            String[] words = message.split("[，。、！？\\s]+");
            for (String word : words) {
                if (word.length() >= 2 && !word.equals("错了") && !word.equals("不对") && 
                    !word.equals("不是") && !word.equals("这个") && !word.equals("那个")) {
                    return word;
                }
            }
        }
        
        return null;
    }

    private String buildContextPrompt(String userQuestion) {
        if (currentAIResult != null) {
            StringBuilder context = new StringBuilder();
            context.append("用户正在询问关于以下植物的问题：\n");
            context.append("植物名称：").append(currentAIResult.getPlantName() != null ? currentAIResult.getPlantName() : "未知").append("\n");
            context.append("健康状态：").append(currentAIResult.getHealthStatus() != null ? currentAIResult.getHealthStatus() : "未知").append("\n");
            context.append("置信度：").append(String.format("%.1f%%", currentAIResult.getConfidence() * 100)).append("\n\n");
            context.append("用户的问题：").append(userQuestion);
            return context.toString();
        }
        return userQuestion;
    }

    private void saveMessage(long sessionId, String content, boolean isUser) {
        if (repository == null) return;
        
        ChatMessageEntity message = new ChatMessageEntity(sessionId, content, isUser);
        repository.insertChatMessage(message, null);
    }

    private void updateSessionLastMessage(long sessionId, String message) {
        if (repository == null) return;
        
        repository.getChatSessionById(sessionId, session -> {
            if (session != null) {
                session.setLastMessage(message.length() > 50 ? message.substring(0, 50) + "..." : message);
                session.setUpdatedAt(System.currentTimeMillis());
                repository.updateChatSession(session);
            }
        });
    }

    private void showProgress(boolean show) {
        if (binding == null) return;
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSend.setEnabled(!show);
        binding.etMessage.setEnabled(!show);
        binding.btnAttachImage.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
