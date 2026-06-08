package com.example.mygraduationproject.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.mygraduationproject.model.ChatMessageEntity;

import java.util.List;

/**
 * 聊天消息数据访问对象，提供chat_messages表的增删改查操作
 * 用于管理花烛、白掌、绿萝等植物AI对话中的具体消息记录
 */
@Dao
public interface ChatMessageDao {

    /** 插入一条聊天消息，返回自增ID */
    @Insert
    long insert(ChatMessageEntity message);

    /** 更新一条聊天消息 */
    @Update
    void update(ChatMessageEntity message);

    /** 删除一条聊天消息 */
    @Delete
    void delete(ChatMessageEntity message);

    /**
     * 根据会话ID获取该会话下的所有消息，按时间正序排列（对话顺序）
     * @param sessionId 所属会话的ID
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    List<ChatMessageEntity> getMessagesBySession(long sessionId);

    /** 统计指定会话下的消息条数 */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE sessionId = :sessionId")
    int getMessageCount(long sessionId);

    /** 删除指定会话下的所有消息（用于清除某次对话的全部记录） */
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    void deleteBySession(long sessionId);

    /** 删除全部聊天消息 */
    @Query("DELETE FROM chat_messages")
    void deleteAll();
}
