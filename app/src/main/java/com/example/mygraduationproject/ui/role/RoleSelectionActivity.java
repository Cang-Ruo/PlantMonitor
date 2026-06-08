package com.example.mygraduationproject.ui.role;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mygraduationproject.MainActivity;
import com.example.mygraduationproject.R;
import com.example.mygraduationproject.databinding.ActivityRoleSelectionBinding;
import com.example.mygraduationproject.mqtt.MqttManager;
import com.example.mygraduationproject.utils.PreferenceManager;

/** 瑙掕壊閫夋嫨椤甸潰锛岄娆″惎鍔ㄦ椂閫夋嫨鐩戞帶绔垨鎺у埗绔鑹?*/
public class RoleSelectionActivity extends AppCompatActivity {

    private static final String TAG = "RoleSelection";
    private ActivityRoleSelectionBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("=== RoleSelectionActivity onCreate ===");
        Log.e(TAG, "=== RoleSelectionActivity onCreate ===");
        
        binding = ActivityRoleSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        
        System.out.println("=== hasSelectedRole: " + preferenceManager.hasSelectedRole() + " ===");
        Log.e(TAG, "hasSelectedRole: " + preferenceManager.hasSelectedRole());
        
        if (preferenceManager.hasSelectedRole()) {
            Log.e(TAG, "已有角色，跳转到 MainActivity");
            navigateToMain();
            return;
        }

        binding.cardMonitor.setOnClickListener(v -> {
            Log.e(TAG, "点击了监控端");
            selectRole(MqttManager.DeviceRole.MONITOR);
        });
        binding.cardController.setOnClickListener(v -> {
            Log.e(TAG, "点击了控制端");
            selectRole(MqttManager.DeviceRole.CONTROLLER);
        });
        
        Log.e(TAG, "RoleSelectionActivity onCreate 完成");
    }
    
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void selectRole(MqttManager.DeviceRole role) {
        Log.e(TAG, "selectRole: " + role.getValue());
        preferenceManager.setDeviceRole(role.getValue());
        
        Toast.makeText(this, "已选择: " + getRoleDisplayName(role), Toast.LENGTH_SHORT).show();
        
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("device_role", role.getValue());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getRoleDisplayName(MqttManager.DeviceRole role) {
        if (role == MqttManager.DeviceRole.MONITOR) {
            return "监控端 (WiFi)";
        } else {
            return "控制端 (4G)";
        }
    }
}
