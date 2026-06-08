package com.example.mygraduationproject.ui.control;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.databinding.FragmentControlBinding;
import com.example.mygraduationproject.model.ControlCommand;
import com.example.mygraduationproject.mqtt.MqttManager;
import com.example.mygraduationproject.utils.PreferenceManager;

/** 鎺у埗绔細琛ュ厜鐏?姘存车/椋庢墖杩滅▼鎺у埗銆佽繙绋嬫媿鐓с€佸畾鏃惰缃?*/
public class ControlFragment extends Fragment {

    private FragmentControlBinding binding;
    private Repository repository;
    private boolean isUpdating = false;
    private MqttManager mqttManager;
    private PreferenceManager preferenceManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentControlBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            repository = Repository.getInstance(getContext());
            preferenceManager = new PreferenceManager(getContext());
            if (getActivity() instanceof MainActivity) {
                mqttManager = ((MainActivity) getActivity()).getMqttManager();
            }
        }
        setupSwitchListeners();
        setupRemoteControlListeners();
        updateMqttStatus();
        loadSavedInterval();
        updateRemoteControlVisibility();
    }
    
    private void updateRemoteControlVisibility() {
        if (binding == null || preferenceManager == null) return;
        
        String role = preferenceManager.getDeviceRole();
        boolean isController = "controller".equals(role);
        
        // 远程拍照控制只在控制端显示
        // 查找包含远程拍照按钮的父CardView
        View btnRemoteCapture = binding.btnRemoteCapture;
        if (btnRemoteCapture != null && btnRemoteCapture.getParent() != null 
            && btnRemoteCapture.getParent() instanceof View) {
            View parentLayout = (View) btnRemoteCapture.getParent();
            if (parentLayout.getParent() != null && parentLayout.getParent() instanceof View) {
                View cardView = (View) parentLayout.getParent();
                cardView.setVisibility(isController ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void loadSavedInterval() {
        if (binding != null && preferenceManager != null) {
            int interval = preferenceManager.getCaptureInterval();
            binding.etCaptureInterval.setText(String.valueOf(interval));
        }
    }

    private void updateMqttStatus() {
        if (binding == null) return;
        
        boolean connected = mqttManager != null && mqttManager.isConnected();
        if (connected) {
            binding.tvMqttStatus.setText("MQTT: 已连接");
            binding.tvMqttStatus.setTextColor(getResources().getColor(R.color.health_good, null));
        } else {
            binding.tvMqttStatus.setText("MQTT: 未连接");
            binding.tvMqttStatus.setTextColor(getResources().getColor(R.color.health_bad, null));
        }
    }

    private void setupSwitchListeners() {
        if (binding == null) return;
        
        binding.switchLight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                sendMqttControlCommand("led", isChecked);
            }
        });
        
        binding.switchPump.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                sendMqttControlCommand("pump", isChecked);
            }
        });
        
        binding.switchFan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdating) {
                sendMqttControlCommand("fan", isChecked);
            }
        });
    }

    private void setupRemoteControlListeners() {
        if (binding == null) return;

        // 立即拍照按钮
        binding.btnRemoteCapture.setOnClickListener(v -> {
            if (mqttManager == null || !mqttManager.isConnected()) {
                Toast.makeText(getContext(), "MQTT未连接，请检查网络", Toast.LENGTH_SHORT).show();
                return;
            }
            
            updateRemoteStatus("正在发送拍照指令...");
            mqttManager.sendRemoteCaptureCommand(new MqttManager.RemoteCommandCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "拍照指令发送成功", Toast.LENGTH_SHORT).show();
                            updateRemoteStatus("拍照指令已发送，监控端将在几秒内执行");
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "发送失败: " + error, Toast.LENGTH_SHORT).show();
                            updateRemoteStatus("发送失败: " + error);
                        });
                    }
                }
            });
        });

        // 保存设置按钮
        binding.btnSaveInterval.setOnClickListener(v -> {
            String intervalStr = binding.etCaptureInterval.getText().toString().trim();
            if (TextUtils.isEmpty(intervalStr)) {
                Toast.makeText(getContext(), "请输入拍照间隔", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int interval;
            try {
                interval = Integer.parseInt(intervalStr);
                if (interval < 1) {
                    Toast.makeText(getContext(), "间隔时间不能小于1分钟", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (interval > 1440) {
                    Toast.makeText(getContext(), "间隔时间不能超过1440分钟(24小时)", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (mqttManager == null || !mqttManager.isConnected()) {
                Toast.makeText(getContext(), "MQTT未连接，请检查网络", Toast.LENGTH_SHORT).show();
                return;
            }
            
            updateRemoteStatus("正在发送间隔设置...");
            mqttManager.sendRemoteIntervalCommand(interval, new MqttManager.RemoteCommandCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (preferenceManager != null) {
                                preferenceManager.setCaptureInterval(interval);
                            }
                            Toast.makeText(getContext(), "间隔设置发送成功", Toast.LENGTH_SHORT).show();
                            updateRemoteStatus("间隔设置已发送(" + interval + "分钟)，监控端将在几秒内更新");
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "发送失败: " + error, Toast.LENGTH_SHORT).show();
                            updateRemoteStatus("发送失败: " + error);
                        });
                    }
                }
            });
        });
        
        binding.switchRemoteTimer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (mqttManager == null || !mqttManager.isConnected()) {
                Toast.makeText(getContext(), "MQTT未连接，请检查网络", Toast.LENGTH_SHORT).show();
                binding.switchRemoteTimer.setChecked(!isChecked);
                return;
            }
            
            String statusText = isChecked ? "正在发送开启定时指令..." : "正在发送关闭定时指令...";
            updateRemoteStatus(statusText);
            mqttManager.sendRemoteTimerToggleCommand(isChecked, new MqttManager.RemoteCommandCallback() {
                @Override
                public void onSuccess() {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), isChecked ? "开启定时指令发送成功" : "关闭定时指令发送成功", Toast.LENGTH_SHORT).show();
                            updateRemoteStatus(isChecked ? "开启定时指令已发送" : "关闭定时指令已发送");
                        });
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "发送失败: " + error, Toast.LENGTH_SHORT).show();
                            updateRemoteStatus("发送失败: " + error);
                            binding.switchRemoteTimer.setChecked(!isChecked);
                        });
                    }
                }
            });
        });
    }

    private void updateRemoteStatus(String status) {
        if (binding != null) {
            binding.tvRemoteStatus.setText(status);
            binding.tvRemoteStatus.setVisibility(View.VISIBLE);
        }
    }

    private void sendMqttControlCommand(String device, boolean state) {
        if (getContext() == null || binding == null) return;
        
        if (mqttManager == null || !mqttManager.isConnected()) {
            Toast.makeText(getContext(), "MQTT未连接，请检查网络", Toast.LENGTH_SHORT).show();
            restoreSwitchState(device, state);
            updateMqttStatus();
            return;
        }
        
        mqttManager.sendControlCommand(device, state);
        
        String deviceName = getDeviceName(device);
        Toast.makeText(getContext(), deviceName + (state ? " 已开启" : " 已关闭"), Toast.LENGTH_SHORT).show();
        
        updateDeviceStatus(device, state);
        
        ControlCommand command = new ControlCommand(device, state ? ControlCommand.ACTION_ON : ControlCommand.ACTION_OFF);
        if (repository != null) {
            repository.insertControlCommand(command, null);
        }
    }

    private String getDeviceName(String device) {
        switch (device) {
            case "led":
                return "补光灯";
            case "pump":
                return "水泵";
            case "fan":
                return "风扇";
            default:
                return device;
        }
    }

    private void updateDeviceStatus(String device, boolean isOn) {
        if (binding == null) return;
        
        String statusText = isOn ? "开启" : "关闭";
        int statusColor = isOn ? R.color.health_good : R.color.health_bad;
        
        TextView statusView = null;
        switch (device) {
            case "led":
                statusView = binding.tvLightStatus;
                break;
            case "pump":
                statusView = binding.tvPumpStatus;
                break;
            case "fan":
                statusView = binding.tvFanStatus;
                break;
        }
        
        if (statusView != null) {
            statusView.setText(statusText);
            statusView.setTextColor(getResources().getColor(statusColor, null));
        }
    }

    private void restoreSwitchState(String device, boolean wasTurnedOn) {
        if (binding == null) return;
        
        isUpdating = true;
        boolean shouldTurnOn = !wasTurnedOn;
        
        switch (device) {
            case "led":
                binding.switchLight.setChecked(shouldTurnOn);
                break;
            case "pump":
                binding.switchPump.setChecked(shouldTurnOn);
                break;
            case "fan":
                binding.switchFan.setChecked(shouldTurnOn);
                break;
        }
        isUpdating = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            mqttManager = ((MainActivity) getActivity()).getMqttManager();
        }
        updateMqttStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mqttManager != null) {
            mqttManager.setRemoteResponseListener(null);
        }
        binding = null;
    }
}
