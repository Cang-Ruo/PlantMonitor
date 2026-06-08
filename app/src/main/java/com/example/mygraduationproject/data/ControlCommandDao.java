package com.example.mygraduationproject.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.ControlCommand;

import java.util.List;

/**
 * 控制命令数据访问对象，提供control_commands表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物养护设备的远程控制命令记录
 */
@Dao
public interface ControlCommandDao {

    /** 插入一条控制命令，返回自增ID */
    @Insert
    long insert(ControlCommand command);

    /** 更新一条控制命令 */
    @Update
    void update(ControlCommand command);

    /** 删除一条控制命令 */
    @Delete
    void delete(ControlCommand command);

    /** 按发送时间倒序获取全部控制命令 */
    @Query("SELECT * FROM control_commands ORDER BY sendTime DESC")
    List<ControlCommand> getAllCommands();

    /** 按发送时间倒序获取最近指定数量的控制命令 */
    @Query("SELECT * FROM control_commands ORDER BY sendTime DESC LIMIT :limit")
    List<ControlCommand> getRecentCommands(int limit);

    /** 根据记录ID获取控制命令 */
    @Query("SELECT * FROM control_commands WHERE id = :id")
    ControlCommand getById(long id);

    /** 根据设备编码获取该设备最新一条控制命令（按发送时间倒序取第一条） */
    @Query("SELECT * FROM control_commands WHERE deviceCode = :deviceCode ORDER BY sendTime DESC LIMIT 1")
    ControlCommand getLatestByDevice(String deviceCode);

    /** 获取控制命令总条数 */
    @Query("SELECT COUNT(*) FROM control_commands")
    int getCount();

    /** 获取执行成功的命令条数（success=1表示成功） */
    @Query("SELECT COUNT(*) FROM control_commands WHERE success = 1")
    int getSuccessCount();

    /**
     * 删除旧命令记录，仅保留最近keepCount条
     * 通过子查询找出最新的keepCount条记录，删除不在其中的旧命令
     */
    @Query("DELETE FROM control_commands WHERE id NOT IN (SELECT id FROM control_commands ORDER BY sendTime DESC LIMIT :keepCount)")
    void deleteOldCommands(int keepCount);

    /** 删除全部控制命令记录 */
    @Query("DELETE FROM control_commands")
    void deleteAll();
}
