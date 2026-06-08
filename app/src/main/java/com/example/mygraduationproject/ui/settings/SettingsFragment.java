package com.example.mygraduationproject.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mygraduationproject.R;
import com.example.mygraduationproject.data.Repository;
import com.example.mygraduationproject.databinding.FragmentSettingsBinding;
import com.example.mygraduationproject.service.CameraService;
import com.example.mygraduationproject.utils.PreferenceManager;
import com.example.mygraduationproject.worker.AutoCaptureWorker;

/** 璁剧疆椤甸潰锛氳澶囪鑹层€佹媿鎽勯棿闅斻€佺綉缁滈厤缃€佺紦瀛樻竻鐞?*/
public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private PreferenceManager preferenceManager;
    private Repository repository;
    
    private String[] intervalUnits;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getContext() != null) {
            preferenceManager = new PreferenceManager(getContext());
            repository = Repository.getInstance(getContext());
        }
        intervalUnits = new String[]{"秒", "分钟"};
        loadSettings();
        loadDeviceRole();
        setupListeners();
    }

    private void loadDeviceRole() {
        if (binding == null || preferenceManager == null) return;
        
        String role = preferenceManager.getDeviceRole();
        if ("monitor".equals(role)) {
            binding.rbMonitor.setChecked(true);
            binding.tvCurrentRole.setText("当前角色：监控端 (WiFi)");
        } else {
            binding.rbController.setChecked(true);
            binding.tvCurrentRole.setText("当前角色：控制端 (4G)");
        }
    }

    private void loadSettings() {
        if (binding == null || preferenceManager == null || getContext() == null) return;
        
        int interval = preferenceManager.getCaptureInterval();
        String unit = preferenceManager.getIntervalUnit();
        updateIntervalSelection(interval, unit);
        
        binding.etDeviceIp.setText(preferenceManager.getDeviceIp());
        binding.etDevicePort.setText(String.valueOf(preferenceManager.getDevicePort()));
        
        setupIntervalSpinner();
        updateCurrentIntervalDisplay();
    }

    private void updateIntervalSelection(int value, String unit) {
        if (binding == null) return;
        
        binding.rgCaptureInterval.clearCheck();
        
        if ("seconds".equals(unit)) {
            binding.etCustomInterval.setText(String.valueOf(value));
            binding.spinnerIntervalUnit.setSelection(0);
        } else {
            switch (value) {
                case 5:
                    binding.rb5Min.setChecked(true);
                    break;
                case 15:
                    binding.rb15Min.setChecked(true);
                    break;
                case 30:
                    binding.rb30Min.setChecked(true);
                    break;
                case 60:
                    binding.rb1Hour.setChecked(true);
                    break;
                default:
                    if (value < 60) {
                        binding.etCustomInterval.setText(String.valueOf(value));
                        binding.spinnerIntervalUnit.setSelection(0);
                    } else {
                        binding.rb15Min.setChecked(true);
                    }
                    break;
            }
        }
    }
    
    private void setupIntervalSpinner() {
        if (binding == null || getContext() == null) return;
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            getContext(),
            android.R.layout.simple_spinner_item,
            intervalUnits
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerIntervalUnit.setAdapter(adapter);
        
        String currentUnit = preferenceManager.getIntervalUnit();
        if ("seconds".equals(currentUnit)) {
            binding.spinnerIntervalUnit.setSelection(0);
        } else {
            binding.spinnerIntervalUnit.setSelection(1);
        }
    }
    
    private void updateCurrentIntervalDisplay() {
        if (binding == null || preferenceManager == null || getContext() == null) return;
        
        int seconds = preferenceManager.getIntervalInSeconds();
        String unitText;
        int displayValue;
        
        if (seconds < 60) {
            unitText = "秒";
            displayValue = seconds;
        } else {
            unitText = "分钟";
            displayValue = seconds / 60;
        }
        
        String display = String.format(getString(R.string.current_interval_display), displayValue, unitText);
        binding.tvCurrentInterval.setText(display);
    }

    private void setupListeners() {
        if (binding == null || preferenceManager == null) return;
        
        // 设备角色切换监听
        binding.rgDeviceRole.setOnCheckedChangeListener((group, checkedId) -> {
            String newRole;
            if (checkedId == R.id.rbMonitor) {
                newRole = "monitor";
                binding.tvCurrentRole.setText("当前角色：监控端 (WiFi)");
            } else {
                newRole = "controller";
                binding.tvCurrentRole.setText("当前角色：控制端 (4G)");
            }
            preferenceManager.setDeviceRole(newRole);
            Toast.makeText(getContext(), "角色已切换，请重启应用生效", Toast.LENGTH_LONG).show();
        });
        
        binding.rgCaptureInterval.setOnCheckedChangeListener((group, checkedId) -> {
            int interval = 15;
            if (checkedId == R.id.rb5Min) {
                interval = 5;
            } else if (checkedId == R.id.rb15Min) {
                interval = 15;
            } else if (checkedId == R.id.rb30Min) {
                interval = 30;
            } else if (checkedId == R.id.rb1Hour) {
                interval = 60;
            }
            preferenceManager.setCaptureInterval(interval);
            preferenceManager.setIntervalUnit("minutes");
            updateCurrentIntervalDisplay();
            
            if (preferenceManager.isAutoCaptureEnabled()) {
                CameraService.stop(getContext());
                CameraService.start(getContext());
                AutoCaptureWorker.cancelAutoCapture(getContext());
            }
        });
        
        binding.etDeviceIp.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && preferenceManager != null) {
                preferenceManager.setDeviceIp(binding.etDeviceIp.getText().toString().trim());
            }
        });
        
        binding.etDevicePort.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && preferenceManager != null) {
                try {
                    int port = Integer.parseInt(binding.etDevicePort.getText().toString().trim());
                    preferenceManager.setDevicePort(port);
                } catch (NumberFormatException e) {
                    binding.etDevicePort.setText(String.valueOf(preferenceManager.getDevicePort()));
                }
            }
        });
        
        binding.btnControlHistory.setOnClickListener(v -> {
        });
        
        binding.btnClearCache.setOnClickListener(v -> {
            clearCache();
        });
        
        binding.btnSetCustomInterval.setOnClickListener(v -> {
            setCustomInterval();
        });
    }

    private void clearCache() {
        if (repository == null) return;
        
        repository.clearAllCache(success -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success && getContext() != null) {
                        Toast.makeText(getContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
    
    private void setCustomInterval() {
        if (binding == null || preferenceManager == null || getContext() == null) return;
        
        try {
            String valueStr = binding.etCustomInterval.getText().toString().trim();
            if (valueStr.isEmpty()) {
                Toast.makeText(getContext(), "请输入数值", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int value = Integer.parseInt(valueStr);
            int selectedUnit = binding.spinnerIntervalUnit.getSelectedItemPosition();
            
            if (value <= 0) {
                Toast.makeText(getContext(), "数值必须大于 0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String unit = (selectedUnit == 0) ? "seconds" : "minutes";
            int maxSeconds = 300;
            int currentSeconds = (selectedUnit == 0) ? value : value * 60;
            
            if (currentSeconds > maxSeconds) {
                Toast.makeText(getContext(), "演示模式下最大间隔为 " + (maxSeconds / 60) + " 分钟", Toast.LENGTH_SHORT).show();
                return;
            }
            
            preferenceManager.setCaptureInterval(value);
            preferenceManager.setIntervalUnit(unit);
            
            updateCurrentIntervalDisplay();
            
            if (preferenceManager.isAutoCaptureEnabled()) {
                CameraService.stop(getContext());
                CameraService.start(getContext());
                
                AutoCaptureWorker.cancelAutoCapture(getContext());
            }
            
            Toast.makeText(getContext(), "间隔已设置为 " + value + " " + intervalUnits[selectedUnit], Toast.LENGTH_SHORT).show();
            
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
