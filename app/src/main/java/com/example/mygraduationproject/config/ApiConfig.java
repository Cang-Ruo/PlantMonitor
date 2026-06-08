package com.example.mygraduationproject.config;

/**
 * API配置常量：百度密钥、通义千问URL、OneNET设备配置
 * 
 * 重要提示：请在下方填入你自己的 API Key 和配置信息
 */
public class ApiConfig {
    
    // ==================== 百度 AI 配置（植物识别）====================
    // 请访问 https://ai.baidu.com/ 注册并获取 API Key
    public static final String BAIDU_API_KEY = "YOUR_BAIDU_API_KEY";
    public static final String BAIDU_SECRET_KEY = "YOUR_BAIDU_SECRET_KEY";
    
    // ==================== 通义千问配置（详细分析）====================
    // 请访问 https://help.aliyun.com/zh/dashscope/ 获取 API Key
    public static final String QWEN_API_KEY = "YOUR_QWEN_API_KEY";
    
    public static final String QWEN_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    
    public static final String QWEN_MODEL = "qwen-plus";
    
    public static final String BAIDU_PLANT_API = "https://aip.baidubce.com/rest/2.0/image-classify/v1/plant";
    public static final String BAIDU_TOKEN_API = "https://aip.baidubce.com/oauth/2.0/token";
    
    public static final String AI_NAME = "小植";
    public static final String AI_GREETING = "你好！我是小植，你的植物养护助手 🌱\n\n我可以帮你：\n• 识别植物种类\n• 诊断病虫害问题\n• 提供养护建议\n\n有什么问题都可以问我哦~";
    
    // ==================== OneNET API 配置 ====================
    // 请访问 https://open.iot.10086.cn/ 注册并创建产品
    public static final String ONENET_API_URL = "https://iot-api.heclouds.com/thingmodel/set-device-property";
    
    // 替换为你的 OneNET 产品 ID
    public static final String ONENET_PRODUCT_ID = "YOUR_PRODUCT_ID";
    
    // 替换为你的设备名称
    public static final String ONENET_DEVICE_NAME = "YOUR_DEVICE_NAME";
    
    // 替换为你的 OneNET 产品 AccessKey
    public static final String ONENET_API_KEY = "YOUR_ONENET_API_KEY";
    
    // 替换为你的 OneNET 用户 ID
    public static final String ONENET_USER_ID = "YOUR_USER_ID";
    
    public static final class QwenModels {
        public static final String QWEN_TURBO = "qwen-turbo";
        public static final String QWEN_PLUS = "qwen-plus";
        public static final String QWEN_MAX = "qwen-max";
        public static final String QWEN_LONG = "qwen-long";
        public static final String QWEN2_5_72B = "qwen2.5-72b-instruct";
        public static final String QWEN2_5_32B = "qwen2.5-32b-instruct";
        public static final String QWEN2_5_14B = "qwen2.5-14b-instruct";
        public static final String QWEN2_5_7B = "qwen2.5-7b-instruct";
    }
}
