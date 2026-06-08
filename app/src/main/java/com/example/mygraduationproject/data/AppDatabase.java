package com.example.mygraduationproject.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.mygraduationproject.model.AIResult;
import com.example.mygraduationproject.model.ChatMessageEntity;
import com.example.mygraduationproject.model.ChatSession;
import com.example.mygraduationproject.model.ControlCommand;
import com.example.mygraduationproject.model.GrowthRecord;
import com.example.mygraduationproject.model.PlantImage;

/**
 * Room数据库定义类，管理所有数据表的创建和版本迁移
 * 当前数据库版本：12，包含6张表
 * 表结构：plant_images、ai_results、control_commands、chat_sessions、chat_messages、growth_records
 */
@Database(entities = {PlantImage.class, AIResult.class, ControlCommand.class, ChatSession.class, ChatMessageEntity.class, GrowthRecord.class}, version = 12, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static volatile AppDatabase INSTANCE; // 单例实例，volatile保证多线程可见性
    private static final String DATABASE_NAME = "plant_monitor_db"; // 数据库文件名
    
    /** 迁移1→2：ai_results表新增imagePath字段 */
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ai_results ADD COLUMN imagePath TEXT");
        }
    };
    
    /** 迁移2→3：ai_results表新增candidates和detailedAnalysis字段 */
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ai_results ADD COLUMN candidates TEXT");
            database.execSQL("ALTER TABLE ai_results ADD COLUMN detailedAnalysis TEXT");
        }
    };
    
    /** 迁移3→4：新增chat_sessions和chat_messages两张表，用于AI对话功能 */
    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS chat_sessions (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "imagePath TEXT, " +
                    "plantName TEXT, " +
                    "lastMessage TEXT, " +
                    "createdAt INTEGER NOT NULL, " +
                    "updatedAt INTEGER NOT NULL)");
            
            database.execSQL("CREATE TABLE IF NOT EXISTS chat_messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "sessionId INTEGER NOT NULL, " +
                    "content TEXT, " +
                    "isUser INTEGER NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE)");
            
            database.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)");
        }
    };
    
    /** 迁移4→5：ai_results表新增环境数据字段（健康评分、水温、气温、湿度） */
    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ai_results ADD COLUMN healthScore REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE ai_results ADD COLUMN waterTemp REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE ai_results ADD COLUMN airTemp REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE ai_results ADD COLUMN airHumidity REAL NOT NULL DEFAULT 0");
        }
    };
    
    /** 迁移5→6：新增growth_records表，用于存储植物生长监测数据 */
    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS growth_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "imageUrl TEXT, " +
                    "airTemp REAL NOT NULL DEFAULT 0, " +
                    "airHumidity REAL NOT NULL DEFAULT 0, " +
                    "waterTemp REAL NOT NULL DEFAULT 0, " +
                    "timestamp INTEGER NOT NULL DEFAULT 0, " +
                    "healthScore REAL NOT NULL DEFAULT 0, " +
                    "plantName TEXT, " +
                    "healthStatus TEXT)");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_growth_records_timestamp ON growth_records(timestamp)");
        }
    };
    
    /** 迁移6→7：重建ai_results表以添加imageId索引，优化关联查询性能 */
    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS ai_results_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "imageId INTEGER NOT NULL, " +
                    "plantName TEXT, " +
                    "confidence REAL NOT NULL, " +
                    "healthStatus TEXT, " +
                    "diseaseName TEXT, " +
                    "diseaseDescription TEXT, " +
                    "rawResult TEXT, " +
                    "userInput TEXT, " +
                    "suggestion TEXT, " +
                    "imagePath TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "candidates TEXT, " +
                    "detailedAnalysis TEXT, " +
                    "healthScore REAL NOT NULL DEFAULT 0, " +
                    "waterTemp REAL NOT NULL DEFAULT 0, " +
                    "airTemp REAL NOT NULL DEFAULT 0, " +
                    "airHumidity REAL NOT NULL DEFAULT 0)");
            database.execSQL("INSERT INTO ai_results_new SELECT * FROM ai_results");
            database.execSQL("DROP TABLE ai_results");
            database.execSQL("ALTER TABLE ai_results_new RENAME TO ai_results");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ai_results_imageId ON ai_results(imageId)");
        }
    };
    
    /** 迁移7→8：ai_results和growth_records表新增plantType字段，支持植物类型分类 */
    private static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE ai_results ADD COLUMN plantType TEXT");
            database.execSQL("ALTER TABLE growth_records ADD COLUMN plantType TEXT");
            database.execSQL("CREATE INDEX IF NOT EXISTS index_growth_records_plantType ON growth_records(plantType)");
        }
    };

    /** 迁移8→9：将"红掌"和"花烛"统一更名为"红烛"（历史名称统一） */
    private static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE ai_results SET plantType = '红烛' WHERE plantType = '红掌'");
            database.execSQL("UPDATE ai_results SET plantType = '红烛' WHERE plantType = '花烛'");
            database.execSQL("UPDATE growth_records SET plantType = '红烛' WHERE plantType = '红掌'");
            database.execSQL("UPDATE growth_records SET plantType = '红烛' WHERE plantType = '花烛'");
        }
    };

    /** 迁移9→10：将空值和"其他"的plantType统一设为"红烛"（默认值填充） */
    private static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE growth_records SET plantType = '红烛' WHERE plantType IS NULL");
            database.execSQL("UPDATE growth_records SET plantType = '红烛' WHERE plantType = '其他'");
            database.execSQL("UPDATE ai_results SET plantType = '红烛' WHERE plantType IS NULL");
        }
    };

    /** 迁移10→11：将"红烛"统一更名为"花烛"，并清除所有旧数据重新采集 */
    private static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("UPDATE ai_results SET plantType = '花烛' WHERE plantType = '红烛'");
            database.execSQL("UPDATE ai_results SET plantType = '花烛' WHERE plantType = '红掌'");
            database.execSQL("UPDATE growth_records SET plantType = '花烛' WHERE plantType = '红烛'");
            database.execSQL("UPDATE growth_records SET plantType = '花烛' WHERE plantType = '红掌'");
            // 清除所有旧数据，重新采集
            database.execSQL("DELETE FROM ai_results");
            database.execSQL("DELETE FROM growth_records");
            database.execSQL("DELETE FROM plant_images");
        }
    };

    /** 迁移11→12：删除growth_records中按时间戳分组的重复记录，保留每组中ID最小的记录 */
    private static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("DELETE FROM growth_records WHERE id NOT IN " +
                    "(SELECT MIN(id) FROM growth_records GROUP BY timestamp)");
        }
    };
    
    /** 获取植物图片数据访问对象 */
    public abstract PlantImageDao plantImageDao();
    /** 获取AI识别结果数据访问对象 */
    public abstract AIResultDao aiResultDao();
    /** 获取控制命令数据访问对象 */
    public abstract ControlCommandDao controlCommandDao();
    /** 获取聊天会话数据访问对象 */
    public abstract ChatSessionDao chatSessionDao();
    /** 获取聊天消息数据访问对象 */
    public abstract ChatMessageDao chatMessageDao();
    /** 获取生长记录数据访问对象 */
    public abstract GrowthRecordDao growthRecordDao();
    
    /** 获取AppDatabase单例，双重检查锁定保证线程安全，注册所有迁移策略 */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
                    .fallbackToDestructiveMigration() // 未定义迁移时销毁重建
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /** 销毁数据库实例，用于完全重置 */
    public static void destroyInstance() {
        INSTANCE = null;
    }
}
